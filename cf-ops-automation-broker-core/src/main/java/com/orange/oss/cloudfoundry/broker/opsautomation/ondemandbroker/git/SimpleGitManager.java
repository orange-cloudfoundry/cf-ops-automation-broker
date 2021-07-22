package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;


/**
 * Git Mediation :
 * clones repo
 * adds modified files
 * commit, rebase then push
 *
 * @author poblin-orange
 */
public class SimpleGitManager implements GitManager {


    protected static final String PRIVATE_GIT_INSTANCE = "private-git-instance";
    private static final Logger logger = LoggerFactory.getLogger(SimpleGitManager.class.getName());
    static final String PRIVATE_SUBMODULES_LIST = "private_submodules_list";

    private final String gitUrl;
    private final String committerName;
    private final String committerEmail;
    private String repoAliasName;

    private final UsernamePasswordCredentialsProvider cred;

    public SimpleGitManager(String gitUser, String gitPassword, String gitUrl, String committerName, String committerEmail, String repoAliasName) {
        this.gitUrl = gitUrl;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
        this.cred = new UsernamePasswordCredentialsProvider(gitUser, gitPassword);
        this.repoAliasName = repoAliasName == null ? "" : repoAliasName;
    }


    @Override
    public void cloneRepo(Context ctx) {
        Path workDir = null;
        try {

            logger.info(prefixLog("cloning repo from {}"), this.gitUrl);

            workDir = Files.createTempDirectory(getRepoWorkDirPrefix(repoAliasName));

            int timeoutSeconds = 55; //git timeout
            CloneCommand clone = new CloneCommand()
                    .setCredentialsProvider(cred)
                    .setDirectory(workDir.toFile())
                    .setTimeout(timeoutSeconds)
                    .setURI(this.gitUrl)
                    .setCloneAllBranches(true); //explicitly set as default is not clearly documented

            Git git = clone.call();
            this.setGit(git, ctx);

            Config config = git.getRepository().getConfig();
            setUserConfig(config);
            configureCrLf(config);

            checkoutRemoteBranchIfNeeded(git, ctx);

            createNewBranchIfNeeded(git, ctx);

            fetchSubmodulesIfNeeded(ctx, git);

            logger.info(prefixLog("git repo is ready at {}"), workDir);
            //push the work dir in invocation context
            setWorkDir(workDir, ctx);
        } catch (Exception e) {
            String msgContext = (workDir == null) ? "" : (" while cloning into dir: " + workDir);
            logger.warn(prefixLog("caught ") + e + msgContext + " Cleaning up local clone.", e);
            deleteWorkingDir(workDir);
            throw new IllegalArgumentException(e);
        }

    }

    /**
     * Overrides local branch with remote. Useful when git repo was pooled.
     */
    @Override
    public void fetchRemoteAndResetCurrentBranch(Context ctx) {
        Git git = getGit(ctx);
        String remoteBranch = getImplicitRemoteBranchToDisplay(ctx);
        //See https://git-scm.com/book/en/v2/Git-Internals-The-Refspec about the refspec/
        //The format of the refspec is:
        // first, an optional +, followed by <src>:<dst>,
        // where:
        // <src> is the pattern for references on the remote side and
        // <dst> is where those references will be tracked locally.
        // The + tells Git to update the reference even if it isn’t a fast-forward.
        String fetchRef = "+refs/heads/" +remoteBranch + ":refs/remotes/" + DEFAULT_REMOTE_NAME + "/" + remoteBranch;
        FetchCommand fetchCommand = git.fetch().
                setCredentialsProvider(cred).
                setRefSpecs(fetchRef).
                setRemote(DEFAULT_REMOTE_NAME);
        String resetRef = DEFAULT_REMOTE_NAME + "/" + remoteBranch;
        ResetCommand resetCommand = git.reset().setRef(resetRef).setMode(ResetCommand.ResetType.HARD);
        try {
            logger.info(prefixLog("fetching from " + fetchRef));
            fetchCommand.call();
            logger.info(prefixLog("resetting from " + resetRef));
            resetCommand.call();
            //Clean the working dir from unstaged files (e.g. input validation fails
            //OSB request before reaching GitProcessor
            Set<String> cleanedPath = git.clean().call();
            logger.info(prefixLog("cleaned working dir paths: " + cleanedPath));
        } catch (Exception e) {
            logger.error(prefixLog("caught ") + e + " while fetching & resetting branch: " + remoteBranch, e);
            throw new RuntimeException(e);
        }
	}

    String getRepoWorkDirPrefix(String repoAliasName) {
        return repoAliasName.replaceAll("[^a-zA-Z0-9]", "-") + "clone-";
    }

    private void fetchSubmodulesIfNeeded(Context ctx, Git git) throws GitAPIException {
        git.submoduleInit().call();
        Map<String, SubmoduleStatus> submodules = git.submoduleStatus().call();
        saveSubModuleListInContext(ctx, submodules);

        boolean fetchSubModules = false;
        if (Boolean.TRUE.equals(ctx.contextKeys.get(getContextKey(GitProcessorContext.fetchAllSubModules)))) {
            fetchSubModules = true;
        }
        Object selectiveModulesToFetch = ctx.contextKeys.get(getContextKey(GitProcessorContext.submoduleListToFetch));
        if (!fetchSubModules && selectiveModulesToFetch instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> submodulesToFetch = (List<String>) selectiveModulesToFetch;
            submodules.keySet().stream()
                    .filter(s -> ! submodulesToFetch.contains(s))
                    .forEach(s -> excludeModuleFromSubModuleUpdate(git, s));
            fetchSubModules = ! submodulesToFetch.isEmpty();
        }
        if (fetchSubModules) {
            Collection<String> result = git.submoduleUpdate().setCredentialsProvider(cred).call();
            logger.debug("submodule update returned: {}", result);
        }
    }

    private void saveSubModuleListInContext(Context ctx, Map<String, SubmoduleStatus> submodules) {
        List<String> submoduleList = new ArrayList<>(submodules.keySet());
        ctx.contextKeys.put(repoAliasName + PRIVATE_SUBMODULES_LIST, submoduleList);
    }

    private void excludeModuleFromSubModuleUpdate(Git git, String submodulePath) {
        StoredConfig config = git.getRepository().getConfig();
        config.setString("submodule", submodulePath, "update", "none"); //does not work because, possibly because JGit bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=521609
        config.unset("submodule", submodulePath, "url");
        try {
            config.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * equivalent of
     * <pre>
     * git branch cassandra #create a local branch
     * git config branch.cassandra.remote origin; git config branch.cassandra.merge refs/heads/cassandra; #configure branch to push to remote with same name
     * git checkout cassandra # checkout
     * </pre>
     */
    private void createNewBranchIfNeeded(Git git, Context ctx) throws GitAPIException, IOException {
        String branchName = getContextValue(ctx, GitProcessorContext.createBranchIfMissing);

        boolean requiredBranchExists = branchName != null;
        if (requiredBranchExists) {
            Optional<Ref> existingMatchingRemoteBranchRef = lookUpRemoteBranch(git, branchName);

            if (existingMatchingRemoteBranchRef.isPresent()) {
                logger.debug(prefixLog("existing remote branch {}"), branchName);
                git.branchCreate()
                        .setName(branchName)
                        .setStartPoint("refs/remotes/" + DEFAULT_REMOTE_NAME+ "/" + branchName)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                        .call();
                logger.info(prefixLog("created local branch from remote branch {}"), branchName);


            } else {
                git.branchCreate()
                        .setName(branchName)
                        .call();

                git.getRepository().getConfig()
                        .setString("branch", branchName, "remote", DEFAULT_REMOTE_NAME);
                git.getRepository().getConfig()
                        .setString("push", branchName, "default", "upstream"); //overkill ?
                git.getRepository().getConfig()
                        .setString("branch", branchName, "merge", "refs/heads/" + branchName);
                git.getRepository().getConfig().save();

                logger.info(prefixLog("created branch {} from current HEAD"), branchName);

                PushCommand pushCommand = git.push().setCredentialsProvider(cred);
                String fetchRef = "refs/heads/" +branchName + ":refs/heads/" +branchName ;

                Iterable<PushResult> pushResults = pushCommand.setRefSpecs(new RefSpec(fetchRef)).call();

                List<RemoteRefUpdate.Status> failedStatuses = extractFailedStatuses(pushResults);
                if (failedStatuses.isEmpty()) {
                    logger.info(prefixLog("pushed new remote branch {}"), branchName);
                } else {
                    logger.error(prefixLog("failed to push new remote branch {}, got failures{}"), branchName, failedStatuses);
                    throw new RuntimeException("failed to push: remote conflict. pull rebased failed:" + failedStatuses);
                }

            }


            logger.info(prefixLog("checked out local branch {}"), branchName);
            git.checkout()
                    .setName(branchName)
                    .call();
        }
    }

    Optional<Ref> lookUpRemoteBranch(Git git, String branchName) throws GitAPIException {
        List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        return branches.stream() 
                .filter(ref -> ref.getName().equals("refs/remotes/"+ DEFAULT_REMOTE_NAME + "/" + branchName))
                .findFirst();
    }

    private Optional<Ref> lookUpLocalBranch(Git git, String branchName) throws GitAPIException {
        List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        return branches.stream()
                .filter(ref -> ref.getName().equals("refs/heads/" + branchName))
                .findFirst();
    }


    String getImplicitRemoteBranchToDisplay(Context ctx) {
        String branch = getContextValue(ctx, GitProcessorContext.createBranchIfMissing);
        if (branch == null) {
            branch = getContextValue(ctx, GitProcessorContext.checkOutRemoteBranch);
        }
        if (branch == null) {
            branch = "master";
        }
        return branch;
    }

    private void checkoutRemoteBranchIfNeeded(Git git, Context ctx) throws GitAPIException {
        String branch = getContextValue(ctx, GitProcessorContext.checkOutRemoteBranch);
        if (branch != null) {
            //Default branch is already present, would fail if asked to create one
            boolean shouldCreateLocalBranch = ! lookUpLocalBranch(git, branch).isPresent();
            git.checkout()
                    .setCreateBranch(shouldCreateLocalBranch).setName(branch) //create local branch
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint(DEFAULT_REMOTE_NAME + "/" + branch) //from remote branch
                    .call();
            logger.info(prefixLog("checked out remote branch {}"), branch);
        }

    }

    void configureCrLf(Config config) {
        config.setString("core", null, "autocrlf", "false");
    }


    private void setUserConfig(Config config) {
        if (this.committerName != null) {
            config.setString("user", null, "name", this.committerName);
        }
        if (this.committerEmail != null) {
            config.setString("user", null, "email", this.committerEmail);
        }
    }

    @Override
    public void commitPushRepo(Context ctx, boolean deleteRepo) {
        try {
            logger.info(prefixLog("commit push"));


            Git git = getGit(ctx);
            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

            Status status = git.status().call();
            Set<String> missing = status.getMissing();
            stageMissingFilesExcludingSubModules(ctx, git, missing);
            status = git.status().call();
            if (!status.getConflicting().isEmpty()) {
                logger.error("Unexpected conflicting files:" + status.getConflicting());
                throw new RuntimeException("Unexpected conflicting files, skipping commit and push");
            } else if (status.hasUncommittedChanges() && (
                    !status.getAdded().isEmpty() ||
                            !status.getChanged().isEmpty() ||
                            !status.getRemoved().isEmpty()
            )) {
                logger.info(prefixLog("staged commit: "
                        + " added:" + status.getAdded()
                        + " changed:" + status.getChanged()
                        + " deleted:" + status.getRemoved() + " with message [" + getCommitMessage(ctx) + "]"
                ));
                CommitCommand commitC = git.commit().setMessage(getCommitMessage(ctx));

                RevCommit revCommit = commitC.call();
                logger.info(prefixLog("committed files in " + revCommit.toString()));

            } else {
                logger.info(prefixLog("No changes to commit."));
            }

            if (hasPendingCommits(git, ctx)) {
                pushCommits(git, ctx);
            } else {
                logger.info(prefixLog("No pending commit, skipping push."));
            }
        } catch (Exception e) {
            logger.warn(prefixLog("caught ") + e, e);
            throw new IllegalArgumentException(e);
        } finally {
            if (deleteRepo) {
                deleteWorkingDir(ctx);
            }
        }

    }

     private void stageMissingFilesExcludingSubModules(Context ctx, Git git, Set<String> missing) throws GitAPIException {
        @SuppressWarnings("unchecked")
        List<String> subModulesList = (List<String>) ctx.contextKeys.get(repoAliasName + PRIVATE_SUBMODULES_LIST);
        for (String missingFilePath : missing) {
            boolean fileMatchesSubModule = false;
            for (String submodulePath : subModulesList) {
                if (missingFilePath.startsWith(submodulePath)) {
                    fileMatchesSubModule = true;
                    break;
                }
            }
            if (fileMatchesSubModule) {
                logger.debug(prefixLog("skipping modified submodule from staging: ") + missingFilePath);
            } else {
                logger.info(prefixLog("staging as deleted: ") + missingFilePath);
                git.rm().addFilepattern(missingFilePath).call();
            }
        }
    }

    boolean hasPendingCommits(Git git, Context ctx) throws IOException {
        Repository repository = git.getRepository();
        BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, getImplicitRemoteBranchToDisplay(ctx));
        logger.info(prefixLog("Preparing to push: behind commits={} ahead_commits={}"), trackingStatus.getBehindCount(), trackingStatus.getAheadCount());
        return trackingStatus.getAheadCount() >0;
    }

    private void pushCommits(Git git, Context ctx) throws GitAPIException {
        logger.info(prefixLog("pushing to {} ..."), getImplicitRemoteBranchToDisplay(ctx));
        PushCommand pushCommand = git.push().setCredentialsProvider(cred);

        Iterable<PushResult> pushResults = pushCommand.call();
        logger.info(prefixLog("pushed ..."));
        List<RemoteRefUpdate.Status> failedStatuses = extractFailedStatuses(pushResults);

        if (! failedStatuses.isEmpty()) {
            if (isPushFailuresRecoverable(failedStatuses, pushResults)) {
                logger.info(prefixLog("Failed to push with status {}"), failedStatuses);
                logger.info(prefixLog("pull and rebasing from origin/{} ..."), getImplicitRemoteBranchToDisplay(ctx));
                PullResult pullRebaseResult = git.pull().setRebase(true).setCredentialsProvider(cred).call();
                String pullBaseResultToString = ToStringBuilder.reflectionToString(pullRebaseResult.getRebaseResult());
                if (!pullRebaseResult.isSuccessful()) {
                    logger.error(prefixLog("Failed to pull rebase: ") + pullBaseResultToString);
                    throw new RuntimeException("failed to push with status=" + failedStatuses
                        + ", pull rebase also failed :" + pullRebaseResult + " (see full details in logs)");
                    //intentionally displays org.eclipse.jgit .transport.FetchResult@27fd0b86
                    //to avoid leaking credentials despite RuntimeException filtering
                }
                logger.info(prefixLog("rebased from origin/{}"), getImplicitRemoteBranchToDisplay(ctx));
                logger.debug(prefixLog("pull details:") + pullBaseResultToString);

                logger.info(prefixLog("re-pushing ..."));
                pushCommand = git.push().setCredentialsProvider(cred);
                Iterable<PushResult> secondPushResults = pushCommand.call();
                logger.info(prefixLog("re-pushed ..."));
                List<RemoteRefUpdate.Status> secondPushFailures = extractFailedStatuses(secondPushResults);
                if (!secondPushFailures.isEmpty()) {
                    logger.error(prefixLog("Failed to re-push with status {} extracted from {}"),
                        secondPushFailures, prettyPrint(secondPushResults));
                    throw new RuntimeException("failed to push: remote conflict. re-push following rebase failed " +
                        "with: secondPushFailures=" + secondPushFailures);
                }
            } else {
                logger.error(prefixLog("unrecoverable push failure: {} with full details: {}"), failedStatuses,
                    prettyPrint(pushResults));
                throw new RuntimeException("unrecoverable push failure : failedStatuses=" + failedStatuses);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(prefixLog("push details: ") + prettyPrint(pushResults));
        }
    }

    private boolean isPushFailuresRecoverable(List<RemoteRefUpdate.Status> failedStatuses,
        Iterable<PushResult> pushResults) {
        if (failedStatuses.contains(RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)) {
            //a commit was recorded at the remote since we fetched remote
            return true;
        }
        if (failedStatuses.contains(RemoteRefUpdate.Status.REJECTED_OTHER_REASON)) {
            /*
             We need to extract RemoteRefUpdate.getMessage() deep from the list of push results
             This was observed through logs, "pretty-printed" as:

             org.eclipse.jgit.transport.PushResult@661012[remoteUpdates={refs/heads/master=RemoteRefUpdate[remoteName=refs/heads/master, REJECTED_OTHER_REASON, 6ebf3085bd7765aa4e66bfd5b
83c73659cbf939b...4929ac06033d292b37c8b87ae03a40a6cf126ea5, fastForward, srcRef=refs/heads/master, message="failed to update ref"]},advertisedRefs={refs/heads/master=Ref[refs/heads/master=6ebf3085bd7765aa
4e66bfd5b83c73659cbf939b], refs/tags/42.0.4=Ref[refs/tags/42.0.4=73e5add38d1deeec37bd3ddbc0f9ac7160dbfb70], refs/tags/44.0.5=Ref[refs/tags/44.0.5=7ee44ea61e67b5b386adf26059774fa33f712385], refs/tags/46.0.
0=Ref[refs/tags/46.0.0=9a657ecd1750650b182aa052d8dcd636cd07d812], refs/tags/46.7.1=Ref[refs/tags/46.7.1=a7c695deed330370319b13aa7cc02f9e4407e2c1], refs/tags/47.0.1=Ref[refs/tags/47.0.1=e01ae72fc9b656e0dc3
708340204170c28766b3e], refs/tags/47.0.2=Ref[refs/tags/47.0.2=254df753234b8831512b370d17ca30035d62d22a], refs/tags/47.0.3=Ref[refs/tags/47.0.3=cad6ee6340928e26693e6b1b2cfcd28fd3625736], refs/tags/48.0.2=R
ef[refs/tags/48.0.2=19b43da31db7726540794b9f3ad247c395279322], refs/tags/51.0.0=Ref[refs/tags/51.0.0=fb2a4446860fc2831f3a13bcbbd9056a2f2dbdfe], refs/tags/51.0.1=Ref[refs/tags/51.0.1=ceec3a5230dfde1051b26e
90088f2f02ef355d95], refs/tags/51.0.2=Ref[refs/tags/51.0.2=90f9a5123a3bd86bbc04315e3b1642ec916ad4be], refs/tags/51.0.3=Ref[refs/tags/51.0.3=926c0d2237b2ee2029320b3f058f088f277967a7], refs/tags/end-bootstr
ap=Ref[refs/tags/end-bootstrap=0130a253292d331906c4a0115b48c5e052a6861e]},uri=https://gitlab-gitlab-k8s.redacted-domain.org/paas_templates_group/paas-templates-secrets.git,updates={refs/
remotes/origin/master=TrackingRefUpdate[refs/heads/master -> refs/remotes/origin/master (forced) 6ebf308..4929ac0]},messageBuffer=error: cannot lock ref 'refs/heads/master': is at a5a5fd1d7547d5c2d09a45a7
da88d1175f498388 but expected 6ebf3085bd7765aa4e66bfd5b83c73659cbf939b

             Since this is hard to unit test, we simply test content of the pretty print instead

             */

            String pushResultsToString = prettyPrint(pushResults).toString();
            //noinspection RedundantIfStatement
            if (pushResultsToString.contains("cannot lock ref")) {
                //a commit is likely currently being recorded by the remote concurrently
                //see https://github.com/orange-cloudfoundry/paas-templates/issues/1223
                return true;
            }
        }
        return false;
    }

    private List<RemoteRefUpdate.Status> extractFailedStatuses(Iterable<PushResult> pushResults) {
        return StreamSupport.stream(pushResults.spliterator(), false) //https://stackoverflow.com/questions/23932061/convert-iterable-to-stream-using-java-8-jdk
                .map(PushResult::getRemoteUpdates)
                .flatMap(Collection::stream) //reduces the Iterable
                .map(RemoteRefUpdate::getStatus)
                .distinct()
                .filter(status -> !RemoteRefUpdate.Status.OK.equals(status))
                .collect(Collectors.toList());
    }

    private List<RemoteRefUpdate.Status> filterOutUptoDateStatus(List<RemoteRefUpdate.Status> statusList) {
        return statusList.stream()
                .filter(status -> !RemoteRefUpdate.Status.UP_TO_DATE.equals(status))
                .collect(Collectors.toList());
    }

    private StringBuilder prettyPrint(Iterable<PushResult> results) {
        StringBuilder sb = new StringBuilder();
        for (Object result : results) {
            sb.append(ToStringBuilder.reflectionToString(result));
            sb.append(" ");
        }
        return sb;
    }

    private String getCommitMessage(Context ctx) {
        String configuredMessage = getContextValue(ctx, GitProcessorContext.commitMessage);
        return configuredMessage == null ? "commit by ondemand broker" : configuredMessage;
    }

    @Override
    public void deleteWorkingDir(Context ctx)  {
        Path workDir = this.getWorkDir(ctx);
        deleteWorkingDir(workDir);
        setWorkDir(null, ctx);
    }

    private void deleteWorkingDir(Path workDir) {
        try {
            // cleaning workDir
            if (workDir != null) {
                logger.info(prefixLog("cleaning-up {} work directory"), workDir);
                Consumer<Path> deleter = file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        logger.warn(prefixLog("Unable to delete file {} details:{}"), file, e.toString());
                    }
                };
                Files.walk(workDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(deleter);
            }
        } catch (IOException e) {
            logger.error("Unable to clean up workdir: " + workDir, e);
        }
    }

    Git getGit(Context ctx) {
        return (Git) ctx.contextKeys.get(repoAliasName + PRIVATE_GIT_INSTANCE);
    }

    private void setGit(Git git, Context ctx) {
        ctx.contextKeys.put(repoAliasName + PRIVATE_GIT_INSTANCE, git);
    }

    private Path getWorkDir(Context ctx) {
        return (Path) ctx.contextKeys.get(getContextKey(GitProcessorContext.workDir));
    }

    private String getContextValue(Context ctx, GitProcessorContext key) {
        return (String) ctx.contextKeys.get(getContextKey(key));
    }

    String getContextKey(GitProcessorContext keyEnum) {
        return repoAliasName + keyEnum.toString();
    }

    private void setWorkDir(Path workDir, Context ctx) {
        ctx.contextKeys.put(getContextKey(GitProcessorContext.workDir), workDir);
    }

    String prefixLog(String logMessage) {
        if ("".equals(this.repoAliasName)) {
            return logMessage;
        } else {
            return "[" + this.repoAliasName + "] " + logMessage;
        }
    }
}
