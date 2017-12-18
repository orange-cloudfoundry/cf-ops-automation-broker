package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

            logger.info("cloning repo");


            String prefix = "broker-";

            Path workDir = Files.createTempDirectory(prefix);

            int timeoutSeconds = 60; //git timeout
            CloneCommand cc = new CloneCommand()
                    .setCredentialsProvider(cred)
                    .setDirectory(workDir.toFile())
                    .setTimeout(timeoutSeconds)
                    .setURI(this.gitUrl);

            Git git = cc.call();
            this.setGit(git, ctx);

            setUserConfig(git);

            checkoutRemoteBranchIfNeeded(git, ctx);

            createNewBranchIfNeeded(git, ctx);

            git.submoduleInit().call();
            git.submoduleUpdate().call();

            logger.info("git repo is ready at {}", workDir);
            //push the work dir in invokation context
            setWorkDir(workDir, ctx);

        } catch (Exception e) {
            logger.warn("caught " + e, e);
            throw new IllegalArgumentException(e);
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
    private void createNewBranchIfNeeded(Git git, Context ctx) throws GitAPIException {
        String branch = getContextValue(ctx, GitProcessorContext.createBranchIfMissing);

        if (branch != null) {
            git.branchCreate()
                    .setName(branch)
                    .call();

            git.getRepository().getConfig()
                    .setString("branch", branch, "remote", "origin");
            git.getRepository().getConfig()
                    .setString("branch", branch, "merge", "refs/heads/" + branch);

            git.checkout()
                    .setName(branch)
                    .call();
            logger.info("created and checked out branch {}", branch);
        }
    }


    private String getImplicitRemoteBranchToDisplay(Context ctx) {
        String branch = getContextValue(ctx, GitProcessorContext.checkOutRemoteBranch);
        return branch == null ? "develop" : branch;
    }

    private void checkoutRemoteBranchIfNeeded(Git git, Context ctx) throws GitAPIException {
        String branch = getContextValue(ctx, GitProcessorContext.checkOutRemoteBranch);
        if (branch != null) {
            git.checkout()
                    .setCreateBranch(true).setName(branch)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint("origin/" + branch)
                    .call();
            logger.info("checked out branch {}", branch);
        }

    }

    protected void setUserConfig(Git git) {
        Config config = git.getRepository().getConfig();
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
            logger.info("commit push");


            Git git = getGit(ctx);
            AddCommand addC = git.add().addFilepattern(".");
            addC.call();

            Status status = git.status().call();
            Set<String> missing = status.getMissing();
            for (String f : missing) {
                logger.info("staging as deleted: " + f);
                git.rm().addFilepattern(f).call();
            }
            status = git.status().call();
            if (status.hasUncommittedChanges()) {
                logger.info("pending commit: " + status.getUncommittedChanges() + ". With deleted:" + status.getRemoved() + " added:" + status.getAdded() + " changed:" + status.getChanged());
                CommitCommand commitC = git.commit().setMessage(getCommitMessage(ctx));

                RevCommit revCommit = commitC.call();
                logger.info("commited files in " + revCommit.toString());

                pushCommits(git, ctx);
            } else {
                logger.info("No changes to commit, skipping push");
            }

            if (deleteRepo) {
                deleteWorkingDir(ctx);
            }
        } catch (Exception e) {
            logger.warn("caught " + e, e);
            throw new IllegalArgumentException(e);
        }

    }

    void pushCommits(Git git, Context ctx) throws GitAPIException {
        logger.info("pushing to {}...", getImplicitRemoteBranchToDisplay(ctx));
        PushCommand pushCommand = git.push().setCredentialsProvider(cred);

        Iterable<PushResult> pushResults = pushCommand.call();
        logger.info("pushed ...");
        List<RemoteRefUpdate.Status> failedStatuses = extractFailedStatuses(pushResults);

        if (failedStatuses.contains(RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD)) {
            logger.info("Failed to push with status {}", failedStatuses);
            logger.info("pull and rebasing from origin/{} ...", getImplicitRemoteBranchToDisplay(ctx));
            PullResult pullRebaseResult = git.pull().call();
            if (!pullRebaseResult.isSuccessful()) {
                logger.info("Failed to pull rebase: " + pullRebaseResult);
                throw new RuntimeException("failed to push: remote conflict. pull rebased failed:" + pullRebaseResult);
            }
            logger.info("rebased from origin/{}", getImplicitRemoteBranchToDisplay(ctx));
            logger.debug("pull details:" + ToStringBuilder.reflectionToString(pullRebaseResult));

            logger.info("re-pushing ...");
            pushCommand = git.push().setCredentialsProvider(cred);
            pushResults = pushCommand.call();
            logger.info("re-pushed ...");
            List<RemoteRefUpdate.Status> secondPushFailures = extractFailedStatuses(pushResults);
            if (!secondPushFailures.isEmpty()) {
                logger.info("Failed to re-push with status {}", failedStatuses);
                throw new RuntimeException("failed to push: remote conflict. pull rebased failed:" + pullRebaseResult);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("push details: " + prettyPrint(pushResults));
        }
    }

    List<RemoteRefUpdate.Status> extractFailedStatuses(Iterable<PushResult> pushResults) {
        return StreamSupport.stream(pushResults.spliterator(), false) //https://stackoverflow.com/questions/23932061/convert-iterable-to-stream-using-java-8-jdk
                .map(PushResult::getRemoteUpdates)
                .flatMap(Collection::stream) //reduces the Iterable
                .map(RemoteRefUpdate::getStatus)
                .distinct()
                .filter(status -> !RemoteRefUpdate.Status.OK.equals(status))
                .collect(Collectors.toList());
    }

    public StringBuilder prettyPrint(Iterable results) {
        StringBuilder sb = new StringBuilder();
        for (Object result : results) {
            sb.append(ToStringBuilder.reflectionToString(result));
            sb.append(" ");
        }
        return sb;
    }

    protected String getCommitMessage(Context ctx) {
        String configuredMessage = getContextValue(ctx, GitProcessorContext.commitMessage);
        return configuredMessage == null ? "commit by ondemand broker" : configuredMessage;
    }

    /**
     * recursively delete working directory
     */
    public void deleteWorkingDir(Context ctx) throws IOException {
        // cleaning workDir
        Path workDir = this.getWorkDir(ctx);
        if (workDir != null) {
            boolean deletesuccessful = FileSystemUtils.deleteRecursively(workDir.toFile());
            if (deletesuccessful) {
                logger.info("cleaned-up {} work directory", workDir);
            } else {
                logger.error("unable to clean up {}", workDir);
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

    Path getWorkDir(Context ctx) {
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
}
