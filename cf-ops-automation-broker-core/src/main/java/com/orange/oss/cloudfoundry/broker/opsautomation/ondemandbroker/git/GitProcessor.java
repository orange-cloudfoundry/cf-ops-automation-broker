package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;


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


    private Git git;
    private UsernamePasswordCredentialsProvider cred;
    private Path workDir;

    public GitProcessor(String gitUser, String gitPassword, String gitUrl) {
        this.gitUser = gitUser;
        this.gitPassword = gitPassword;
        this.gitUrl = gitUrl;
    }

    @Override
    public void preCreate(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postCreate(Context ctx) {
        this.commitPushRepo(ctx);
    }

    @Override
    public void preGetLastCreateOperation(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postGetLastCreateOperation(Context ctx) {
        this.commitPushRepo(ctx);
    }

    @Override
    public void preBind(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postBind(Context ctx) {
        this.commitPushRepo(ctx);
    }

    @Override
    public void preDelete(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postDelete(Context ctx) {
        this.commitPushRepo(ctx);
    }

    @Override
    public void preUnBind(Context ctx) {
        this.cloneRepo(ctx);
    }

    @Override
    public void postUnBind(Context ctx) {
        this.commitPushRepo(ctx);
    }

    /**
     * local clone a repo
     *
     * @param ctx exposing the workDir Path in context
     */
    private void cloneRepo(Context ctx) {
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

    /**
     * commit, rebase the push the modification
     */
    private void commitPushRepo(Context ctx) {
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
            logger.info("pending commit: " +  status.getUncommittedChanges() + ". With deleted:" + status.getRemoved() + " added:" + status.getAdded() + " changed:" + status.getChanged());


            CommitCommand commitC = git.commit().setMessage("commit by ondemand broker");
            RevCommit revCommit = commitC.call();
            logger.info("commited files in " + revCommit.toString());

            //TODO: rebase

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
                logger.debug("push details"+ sb.toString());

            }
            deleteRecursiveDir(workDir);
        } catch (Exception e) {
            logger.warn("caught " + e, e);
            throw new IllegalArgumentException(e);
        }

    }

    /**
     * recursive directory delete
     */
    private void deleteRecursiveDir(Path workDir) throws IOException {
        // cleaning workDir
        Files.walkFileTree(workDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        logger.info("cleaned-up {} work directory", workDir);
    }

    //support unit tests
    Git getGit() {
        return git;
    }
}
