package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.Daemon;
import org.eclipse.jgit.transport.DaemonClient;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple git server serving anynymous git: protocol
 *
 * following command should succeed:
 * (rm -rf repo/; git clone -vv git://127.0.0.1/repo.git; cd repo; git log; echo toto > toto.txt; git add toto.txt; git commit -a -m "msg"; git push; git log)
 *
 * Inspired from https://stackoverflow.com/questions/14360909/jgit-how-to-use-inmemoryrepository-to-learn-about-dfs-implementations
 * and https://github.com/centic9/jgit-cookbook/blob/c0f0d591839382461119fc5bd7a73db109d795b0/httpserver/src/main/java/org/dstadler/jgit/server/Main.java#L79-L94 (latter example is not accepting writes and pulls netty making it slower)
 */
public class GitServer {
    private Map<String, Repository> repositories = new HashMap<>();
    private Daemon server;


    public void startLocalEmptyReposServer() throws IOException, GitAPIException {
        this.server = new Daemon(new InetSocketAddress(9418));
        this.server.getService("git-receive-pack").setEnabled(true);
        this.server.setRepositoryResolver(new RepositoryResolverImplementation());
        this.server.start();
    }

    public void stopLocalEmptyReposServer() throws InterruptedException {
        this.server.stopAndWait();
    }

    private final class RepositoryResolverImplementation implements
            RepositoryResolver<DaemonClient> {
        @Override
        public Repository open(DaemonClient client, String name)
                throws RepositoryNotFoundException,
                ServiceNotAuthorizedException, ServiceNotEnabledException,
                ServiceMayNotContinueException {
            Repository repo = repositories.get(name);
            if (repo == null) {

                try {
                    Path workDir = Files.createTempDirectory("GitTestSetup");
                    repo = FileRepositoryBuilder.create(new File(workDir.resolve(name).toFile(), ".git"));
                    repo.create();
//                    populateRepository(repo);
                    Git git = new Git(repo);
                    git.commit().setMessage("Initial empty repo setup").call();
                    git.close();
                    repositories.put(name, repo);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
            return repo;
        }
    }
}
