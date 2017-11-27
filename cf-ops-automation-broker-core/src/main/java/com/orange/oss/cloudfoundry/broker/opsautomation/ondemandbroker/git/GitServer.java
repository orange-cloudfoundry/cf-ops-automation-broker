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
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A simple git server serving anynymous git: protocol
 *
 * following command should succeed:
 * (rm -rf repo/; git clone -vv git://127.0.0.1/repo.git; cd repo; git log; echo toto > toto.txt; git add toto.txt; git commit -a -m "msg"; git push; git log)
 *
 * Inspired from https://stackoverflow.com/questions/14360909/jgit-how-to-use-inmemoryrepository-to-learn-about-dfs-implementations
 * and https://github.com/centic9/jgit-cookbook/blob/c0f0d591839382461119fc5bd7a73db109d795b0/httpserver/src/main/java/org/dstadler/jgit/server/Main.java#L79-L94 (latter example is not accepting writes and pulls netty making it slower)
 *
 * This is shipped in production code to be shared with other submodules in a cheap way.
 */
public class GitServer {
    public static final Consumer<Git> NO_OP_INITIALIZER = git -> { };
    private Map<String, Repository> repositories = new HashMap<>();
    private Daemon server;

    public GitServer() {
    }

    /**
     * Start a git server, by default serving empty git repo
     * @param repoInitializer optional way to initialize the git repo on the fly
     *                        If not needed, use #NO_OP_INITIALIZER as a convenience
     */
    public void startLocalEmptyReposServer(Consumer<Git> repoInitializer) throws IOException, GitAPIException {
        this.server = new Daemon(new InetSocketAddress(9418));
        this.server.getService("git-receive-pack").setEnabled(true);
        this.server.setRepositoryResolver(new RepositoryResolverImplementation(repoInitializer));
        this.server.start();
    }



    public void stopLocalEmptyReposServer() throws InterruptedException {
        cleanUpRepos();
        this.server.stopAndWait();
    }

    public void cleanUpRepos() {
        for (Repository repository : repositories.values()) {
            File workTree = repository.getWorkTree();
            FileSystemUtils.deleteRecursively(workTree.getParentFile());
        }
        repositories.clear();
    }

    private final class RepositoryResolverImplementation implements
            RepositoryResolver<DaemonClient> {

        Consumer<Git> repoInitStep;

        public RepositoryResolverImplementation(Consumer<Git> repoInitStep) {
            this.repoInitStep = repoInitStep;
        }

        @Override
        public Repository open(DaemonClient client, String name)
                throws RepositoryNotFoundException,
                ServiceNotAuthorizedException, ServiceNotEnabledException,
                ServiceMayNotContinueException {
            Repository repo = repositories.get(name);
            if (repo == null) {
                try {
                    Path workDir = Files.createTempDirectory("GitTestSetup");
                    //Note: we use local disk because RepositoryBuilder does the heavy lifting of repository init
                    //and we can debug using local disk more easily if needed
                    repo = FileRepositoryBuilder.create(new File(workDir.resolve(name).toFile(), ".git"));
                    repo.create();
                    Git git = new Git(repo);

                    Consumer<Git> combinedConsumer = repoInitStep.andThen(this::addInitialCommit);
                    combinedConsumer.accept(git);

                    git.close();
                    repositories.put(name, repo);
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }
            return repo;
        }

        /**
         * When missing then master branch is missing and can't be checked out in clones.
         */
        public void addInitialCommit(Git git) {
            try {
                git.commit().setMessage("Initial empty repo setup").call();
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
