package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.GitClient;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediation;

public class GitMediation extends DefaultBrokerMediation {

	private static Logger logger = LoggerFactory.getLogger(GitMediation.class.getName());

	private String gitUser;
	private String gitPassword;
	private String gitUrl;

	private GitClient gitClient;

	private Git git;
	private UsernamePasswordCredentialsProvider cred;
	private Path workDir;

	public GitMediation(String gitUser, String gitPassword, String gitUrl) {
		this.gitUser = gitUser;
		this.gitPassword = gitPassword;
		this.gitUrl = gitUrl;
	}

	@Override
	public void preCreate() {
		this.cloneRepo();
	}

	@Override
	public void postCreate() {
		this.commitPushRepo();
	}

	@Override
	public void preBind() {
		// TODO Auto-generated method stub
		super.preBind();
	}

	@Override
	public void postBind() {
		// TODO Auto-generated method stub
		super.postBind();
	}

	@Override
	public void preDelete() {
		// TODO Auto-generated method stub
		super.preDelete();
	}

	@Override
	public void postDelete() {
		// TODO Auto-generated method stub
		super.postDelete();
	}

	@Override
	public void preUnBind() {
		// TODO Auto-generated method stub
		super.preUnBind();
	}

	@Override
	public void postUnBind() {
		// TODO Auto-generated method stub
		super.postUnBind();
	}

	/**
	 * local clone a repo
	 * 
	 * @throws IOException
	 * @throws GitAPIException
	 * @throws TransportException
	 * @throws InvalidRemoteException
	 */
	private void cloneRepo() {
		try {
			this.cred = new UsernamePasswordCredentialsProvider(this.gitUser, this.gitPassword);
			this.gitClient = new GitClient(this.gitUrl, cred);

			String prefix = "broker-";

			workDir = Files.createTempDirectory(prefix);

			CloneCommand cc = new CloneCommand().setCredentialsProvider(cred).setDirectory(workDir.toFile())
					.setTimeout(15).setURI(this.gitUrl);

			Git git = cc.call();

			String branch = "master";
			git.checkout().setName(branch).call();
			git.submoduleInit().call();
			git.submoduleUpdate().call();

			logger.info("git repo is ready, on branch {}", branch);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);

		}

	}

	/**
	 * commit, rebase the push the modification
	 * 
	 * @throws GitAPIException
	 * @throws NoFilepatternException
	 * @throws IOException
	 */
	private void commitPushRepo() {
		try {
			logger.info("Stop templating");
			AddCommand addC = git.add().addFilepattern(".");
			addC.call();
			logger.info("added files");

			CommitCommand commitC = git.commit().setMessage("commit by ondemand broker");
			commitC.call();
			logger.info("commited files");

			logger.info("pushing ...");
			PushCommand pushCommand = git.push().setCredentialsProvider(cred);
			pushCommand.call();
			logger.info("pushed ...");
			deleteRecursiveDir(workDir);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);

		}

	}

	/**
	 * recursive directory delete
	 * 
	 * @param workDir
	 * @throws IOException
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
		logger.info("deleted {} work directory", workDir);
	}

}
