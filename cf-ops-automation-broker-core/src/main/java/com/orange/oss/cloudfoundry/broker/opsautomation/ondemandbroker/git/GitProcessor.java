package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;


/**
 * Git Mediation :
 * clones repo
 * adds modified files
 * commit, rebase then push
 *
 * @author poblin-orange
 */
public class GitProcessor extends DefaultBrokerProcessor {


    private static Logger logger = LoggerFactory.getLogger(GitProcessor.class.getName());

    private String gitUser;
    private String gitPassword;
    private String gitUrl;
    private String committerName;
    private String committerEmail;


    private Git git;
    private UsernamePasswordCredentialsProvider cred;
    private Path workDir;

    public GitProcessor(String gitUser, String gitPassword, String gitUrl, String committerName, String committerEmail) {
        this.gitUser = gitUser;
        this.gitPassword = gitPassword;
        this.gitUrl = gitUrl;
        this.committerName = committerName;
        this.committerEmail = committerEmail;
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
    public void preGetLastCreateOperation(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postGetLastCreateOperation(Context ctx) {
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
            this.cred = new UsernamePasswordCredentialsProvider(this.gitUser, this.gitPassword);


            String prefix = "broker-";

            workDir = Files.createTempDirectory(prefix);

            int timeoutSeconds = 60; //git timeout
            CloneCommand cc = new CloneCommand()
                    .setCredentialsProvider(cred)
                    .setDirectory(workDir.toFile())
                    .setTimeout(timeoutSeconds)
                    .setURI(this.gitUrl);

            this.git = cc.call();

            setUserConfig();

            String branch = "master";
            git.checkout().setName(branch).call();
            git.submoduleInit().call();
            git.submoduleUpdate().call();

            logger.info("git repo is ready at {}, on branch {} at {}", workDir, branch);
            //push the work dir in invokation context
            ctx.contextKeys.put(GitProcessorContext.workDir.toString(), workDir);


        } catch (Exception e) {
            logger.warn("caught " + e, e);
            throw new IllegalArgumentException(e);

        }

    }

    protected void setUserConfig() {
        Config config = this.git.getRepository().getConfig();
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
                logger.info("pending commit: " +  status.getUncommittedChanges() + ". With deleted:" + status.getRemoved() + " added:" + status.getAdded() + " changed:" + status.getChanged());
                CommitCommand commitC = git.commit().setMessage(getCommitMessage(ctx));

                RevCommit revCommit = commitC.call();
                logger.info("commited files in " + revCommit.toString());

                //TODO: handle conflicts and automatically perform a git rebase

                logger.info("pushing ...");
                PushCommand pushCommand = git.push().setCredentialsProvider(cred);
                Iterable<PushResult> pushResults = pushCommand.call();
                logger.info("pushed ...");
                if (logger.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    for (PushResult pushResult : pushResults) {
                        sb.append(ToStringBuilder.reflectionToString(pushResult));
                        sb.append(" ");
                    }
                    logger.debug("push details: "+ sb.toString());

                }
            } else {
                logger.info("No changes to commit, skipping push");
            }

            if (deleteRepo) {
                deleteWorkingDir();
            }
        } catch (Exception e) {
            logger.warn("caught " + e, e);
            throw new IllegalArgumentException(e);
        }

    }

    protected String getCommitMessage(Context ctx) {
        String configuredMessage = (String) ctx.contextKeys.get(GitProcessorContext.commitMessage.toString());
        return configuredMessage == null ? "commit by ondemand broker" : configuredMessage;
    }

    /**
     * recursively delete working directory
     */
    public void deleteWorkingDir() throws IOException {
        // cleaning workDir
        boolean deletesuccessful = FileSystemUtils.deleteRecursively(this.workDir.toFile());
        if (deletesuccessful) {
            logger.info("cleaned-up {} work directory", this.workDir);
        } else {
            logger.error("unable to clean up {}", this.workDir);
        }
    }

    //support unit tests
    Git getGit() {
        return git;
    }
}
