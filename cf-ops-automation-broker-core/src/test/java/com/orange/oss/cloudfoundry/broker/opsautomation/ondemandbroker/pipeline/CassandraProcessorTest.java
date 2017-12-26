package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.TestCase.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CassandraProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(CassandraProcessorTest.class);

    private final static String ROOT_DEPLOYMENT = "coab-depls";

    @Test
    @Ignore
    public void shouldCreateDeploymentFolders() {

        //Given
        String tmpdir = System.getProperty("java.io.tmpdir");
        System.out.println("The default value of the java.io.tmpdir system property is: \"" + tmpdir + "\"\n");

        Path workDir = null;
        try {
            Context ctx = new Context();
            workDir = Files.createTempDirectory("paas-secrets");
            ctx.contextKeys.put(GitProcessorContext.workDir.toString(),workDir);
            CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                    "plan_id",
                    "org_id",
                    "space_id",
                    null
            );
            request.withServiceInstanceId("guid");

            //and the context being injected to a cloudflare processor
            ctx.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);

            File deploymentDirectory=new File(String.valueOf(workDir)+"/"+ ROOT_DEPLOYMENT +"/"+ "cassandra_" + "guid");
            File deploymentSecretsDirectory=new File(String.valueOf(workDir)+"/"+ ROOT_DEPLOYMENT +"/"+ "cassandra_" + "guid" +"/" + "secrets");


            CassandraProcessor cassandraProcessor = new CassandraProcessor();

            //When
            cassandraProcessor.preCreate(ctx);

            //Then
            assertTrue(deploymentDirectory.exists());


        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}