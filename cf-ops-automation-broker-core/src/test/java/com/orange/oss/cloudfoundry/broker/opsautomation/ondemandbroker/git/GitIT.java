package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * This tests may work standalone, simulating a git server by embedding one.
 * It may otherwise also be run with a locally modified version of git.properties pointing to a private git server:
 * This supports asserting that working with paas-template works well.
 * 
 * Hint: create a private fork of paas-template repo for running this test, to avoid pulluting the repo
 * shared with the rest of the team members
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:git.properties")
@EnableConfigurationProperties({GitProperties.class})
public class GitIT {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    GitProperties gitProperties;

    @Test
    public void testGitProcessor() {

        SimpleGitManager simpleGitManager = new SimpleGitManager(gitProperties.getUser(), gitProperties.getPassword(), gitProperties.getUrl(), gitProperties.committerName(), gitProperties.committerEmail(), null);
        BrokerProcessor paasTemplateSelector = new DefaultBrokerProcessor() {
            @Override
            public void preCreate(Context ctx) {
                ctx.contextKeys.put(GitProcessorContext.checkOutRemoteBranch.toString(), "develop");
                ctx.contextKeys.put(GitProcessorContext.createBranchIfMissing.toString(), "feature-COAB-cassandra-IT");
                ctx.contextKeys.put(GitProcessorContext.submoduleListToFetch.toString(), Collections.singletonList("mysql-deployment"));
            }

        };
        BrokerProcessor paasTemplateGenerator = new DefaultBrokerProcessor() {
            @Override
            public void postCreate(Context ctx) {
                Path workDir = (Path) ctx.contextKeys.get(GitProcessorContext.workDir.toString());
                try {
                    createDir(workDir.resolve("coab-depls").resolve("service-instance-guid"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        List<BrokerProcessor> processors = new ArrayList<>();
        processors.add(paasTemplateSelector);
        processors.add(new GitProcessor(simpleGitManager, null));
        processors.add(paasTemplateGenerator);
        ProcessorChain chain = new ProcessorChain(processors, new DefaultBrokerSink());

        Context ctx = new Context();
        chain.create(ctx);


        //TODO: assert the files are properly pushed to feature-COAB-cassandra-IT branch
    }

    private GitServer gitServer;

    @BeforeEach
    public void startGitServer() throws IOException {
        gitServer = new GitServer();

        Consumer<Git> initPaasTemplate = this::initPaasTemplate;
        gitServer.startEphemeralReposServer(initPaasTemplate);
    }

    public static void createDir(Path dir) throws IOException {
        Files.createDirectories(dir);
        //TODO: create .gitignore
        try (Writer writer = new FileWriter(dir.resolve(".gitkeep").toFile())) {
            writer.write("Please keep me");
        }
    }

    @AfterEach
    public void cleanUpGit() throws Exception {
        gitServer.stopAndCleanupReposServer();
    }

    public void initPaasTemplate(Git git) {
        File gitWorkDir = git.getRepository().getDirectory().getParentFile();
        try {
            git.commit().setMessage("Initial empty repo setup").call();
            String repoName = git.getRepository().getDirectory().toPath().getParent().getFileName().toString();
            if ("volatile-repo.git".equals(repoName)) {
                //In develop branch
                git.checkout().setName("develop").setCreateBranch(true).call();

                //root deployment
                Path coabDepls = gitWorkDir.toPath().resolve("coab-depls");
                createDir(coabDepls);
                //sub deployments
                createDir(coabDepls.resolve("cassandra"));

                AddCommand addC = git.add().addFilepattern(".");
                addC.call();

                git.submoduleInit().call();
                git.submoduleAdd().setPath("bosh-deployment").setURI(gitProperties.getReplicatedSubModuleBasePath() + "bosh-deployment.git").call();
                git.submoduleAdd().setPath("mysql-deployment").setURI(gitProperties.getReplicatedSubModuleBasePath() + "mysql-deployment.git").call();
                git.commit().setMessage("GitIT#startGitServer").call();

                git.checkout().setName("master").call();
            }
            if ("mysql-deployment.git".equals(repoName)) {
                createDir(gitWorkDir.toPath().resolve("mysql-templates"));
                AddCommand addC = git.add().addFilepattern(".");
                addC.call();
                git.commit().setMessage("GitIT#startGitServer").call();
            }
            if ("bosh-deployment.git".equals(repoName)) {
                createDir(gitWorkDir.toPath().resolve("bosh-templates"));
                AddCommand addC = git.add().addFilepattern(".");
                addC.call();
                git.commit().setMessage("GitIT#startGitServer").call();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
