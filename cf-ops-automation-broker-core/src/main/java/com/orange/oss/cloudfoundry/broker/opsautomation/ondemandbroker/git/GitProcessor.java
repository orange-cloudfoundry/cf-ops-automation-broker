package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Git Mediation :
 * clones repo
 * adds modified files
 * commit, rebase then push
 *
 * @author poblin-orange
 */
public class GitProcessor extends DefaultBrokerProcessor {


    private static final String PRIVATE_GIT_INSTANCE = "private-git-instance";
    private static final Logger logger = LoggerFactory.getLogger(GitProcessor.class.getName());
    static final String PRIVATE_SUBMODULES_LIST = "private_submodules_list";

    private final String gitUrl;
    private final String committerName;
    private final String committerEmail;
    private String repoAliasName;

    private final UsernamePasswordCredentialsProvider cred;

    public GitProcessor(String gitUser, String gitPassword, String gitUrl, String committerName, String committerEmail, String repoAliasName) {
        this.gitUrl = gitUrl;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
        this.cred = new UsernamePasswordCredentialsProvider(gitUser, gitPassword);
        this.repoAliasName = repoAliasName == null ? "" : repoAliasName;
    }

    @Override
    public void preCreate(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postCreate(Context ctx) {
        this.commitPushRepo(ctx, true);
    }

    @Override
    public void preGetLastOperation(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postGetLastOperation(Context ctx) {
        this.commitPushRepo(ctx, true);
    }

    @Override
    public void preBind(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postBind(Context ctx) {
        this.commitPushRepo(ctx, true);
    }

    @Override
    public void preDelete(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postDelete(Context ctx) {
        this.commitPushRepo(ctx, true);
    }

    @Override
    public void preUnBind(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postUnBind(Context ctx) {
        this.commitPushRepo(ctx, true);
    }

    /**
     * local clone a repo
     *
     * @param ctx exposing the workDir Path in context
     */
    void cloneRepo(Context ctx) {
        try {

            logger.info(prefixLog("cloning repo from {}"), this.gitUrl);


            String prefix = "broker-";

            Path workDir = Files.createTempDirectory(prefix);

            int timeoutSeconds = 60; //git timeout
            CloneCommand clone = new CloneCommand()
                    .setCredentialsProvider(cred)
                    .setDirectory(workDir.toFile())
                    .setTimeout(timeoutSeconds)
                    .setURI(this.gitUrl);

            Git git = clone.call();
            this.setGit(git, ctx);

            Config config = git.getRepository().getConfig();
            setUserConfig(config);
            configureCrLf(config);

            checkoutRemoteBranchIfNeeded(git, ctx);

            createNewBranchIfNeeded(git, ctx);

            fetchSubmodulesIfNeeded(ctx, git);

            logger.info(prefixLog("git repo is ready at {}"), workDir);
            //push the work dir in invokation context
            setWorkDir(workDir, ctx);

        } catch (Exception e) {
            logger.warn(prefixLog("caught ") + e, e);
            throw new IllegalArgumentException(e);
        }

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
            git.submoduleUpdate().setCredentialsProvider(cred).call();
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
        String branch = getContextValue(ctx, GitProcessorContext.createBranchIfMissing);

        if (branch != null) {
            git.branchCreate()
                    .setName(branch)
                    .call();

            git.getRepository().getConfig()
                    .setString("branch", branch, "remote", "origin");
            git.getRepository().getConfig()
                    .setString("push", branch, "default", "upstream"); //overkill ?
            git.getRepository().getConfig()
                    .setString("branch", branch, "merge", "refs/heads/" + branch);
            git.getRepository().getConfig().save();
            git.checkout()
                    .setName(branch)
                    .call();
            logger.info(prefixLog("created and checked out branch {}"), branch);
        }
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
            git.checkout()
                    .setCreateBranch(true).setName(branch)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint("origin/" + branch)
                    .call();
            logger.info(prefixLog("checked out branch {}"), branch);
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

    /**
     * commit, rebase the push the modification
     */
    void commitPushRepo(Context ctx, boolean deleteRepo) {
        try {
            logger.info(prefixLog("commit push"));


            Git git = getGit(ctx);
            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

            Status status = git.status().call();
            Set<String> missing = status.getMissing();
            stageMissingFilesExcludingSubModules(ctx, git, missing);
            status = git.status().call();
            if (status.hasUncommittedChanges()) {
                logger.info(prefixLog("staged commit:  deleted:") + status.getRemoved() + " added:" + status.getAdded() + " changed:" + status.getChanged());
                CommitCommand commitC = git.commit().setMessage(getCommitMessage(ctx));

                RevCommit revCommit = commitC.call();
                logger.info(prefixLog("commited files in " + revCommit.toString()));

                pushCommits(git, ctx);
            } else {
                logger.info(prefixLog("No changes to commit, skipping push"));
            }

            if (deleteRepo) {
                deleteWorkingDir(ctx);
            }
        } catch (Exception e) {
            logger.warn(prefixLog("caught ") + e, e);
            throw new IllegalArgumentException(e);
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

    private void pushCommits(Git git, Context ctx) throws GitAPIException {
        logger.info(prefixLog("pushing to {} ..."), getImplicitRemoteBranchToDisplay(ctx));
        PushCommand pushCommand = git.push().setCredentialsProvider(cred);

        Iterable<PushResult> pushResults = pushCommand.call();
        logger.info(prefixLog("pushed ..."));
        List<RemoteRefUpdate.Status> failedStatuses = extractFailedStatuses(pushResults);

        if (failedStatuses.contains(RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)) {
            logger.info(prefixLog("Failed to push with status {}"), failedStatuses);
            logger.info(prefixLog("pull and rebasing from origin/{} ..."), getImplicitRemoteBranchToDisplay(ctx));
            PullResult pullRebaseResult = git.pull().setCredentialsProvider(cred).call();
            if (!pullRebaseResult.isSuccessful()) {
                logger.info(prefixLog("Failed to pull rebase: ") + pullRebaseResult);
                throw new RuntimeException("failed to push: remote conflict. pull rebased failed:" + pullRebaseResult);
            }
            logger.info(prefixLog("rebased from origin/{}"), getImplicitRemoteBranchToDisplay(ctx));
            logger.debug(prefixLog("pull details:") + ToStringBuilder.reflectionToString(pullRebaseResult));

            logger.info(prefixLog("re-pushing ..."));
            pushCommand = git.push().setCredentialsProvider(cred);
            pushResults = pushCommand.call();
            logger.info(prefixLog("re-pushed ..."));
            List<RemoteRefUpdate.Status> secondPushFailures = extractFailedStatuses(pushResults);
            if (!secondPushFailures.isEmpty()) {
                logger.info(prefixLog("Failed to re-push with status {}"), failedStatuses);
                throw new RuntimeException("failed to push: remote conflict. pull rebased failed:" + pullRebaseResult);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(prefixLog("push details: ") + prettyPrint(pushResults));
        }
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

    /**
     * recursively delete working directory
     */
    void deleteWorkingDir(Context ctx) throws IOException {
        // cleaning workDir
        Path workDir = this.getWorkDir(ctx);
        if (workDir != null) {
            boolean deletesuccessful = FileSystemUtils.deleteRecursively(workDir.toFile());
            if (deletesuccessful) {
                logger.info(prefixLog("cleaned-up {} work directory"), workDir);
            } else {
                logger.error(prefixLog("unable to clean up {}"), workDir);
            }
            setWorkDir(null, ctx);
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
