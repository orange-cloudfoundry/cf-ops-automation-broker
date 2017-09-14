package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class GitClient {

	private static Logger logger = LoggerFactory.getLogger(GitClient.class);
	private String gitBaseUrl;
	private UsernamePasswordCredentialsProvider cred;

	@Autowired
	List<DeploymentTemplate> templates;
	
	
	
	
	public GitClient(String gitBaseUrl, UsernamePasswordCredentialsProvider cred) {
		this.gitBaseUrl = gitBaseUrl;
		this.cred = cred;
	}

	public void addPipeline(String pipelineName)
			throws InvalidRemoteException, TransportException, GitAPIException, IOException {

		String prefix = "broker-";

		Path workDir = Files.createTempDirectory(prefix);// , attrs);
		try {
			CloneCommand cc = new CloneCommand()
					.setCredentialsProvider(cred)
					.setDirectory(workDir.toFile())
					.setTimeout(15)
					.setURI(this.gitBaseUrl);
			
			Git git = cc.call();

			String branch = "master";
			git.checkout().setName(branch).call();
			git.submoduleInit().call();
			git.submoduleUpdate().call();

			logger.info("git repo is ready, on branch {}", branch);
			
			logger.info("Start templating");
			
			//invoke all Deployment Template beans
			for (DeploymentTemplate template:this.templates){
				template.createDeploymentTemplate();
			}

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
		} finally {

			deleteRecursiveDir(workDir);
		}
	}
	
	/**
	 * recursive directory delete
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

	public void deletePipeline() {

	}

}
