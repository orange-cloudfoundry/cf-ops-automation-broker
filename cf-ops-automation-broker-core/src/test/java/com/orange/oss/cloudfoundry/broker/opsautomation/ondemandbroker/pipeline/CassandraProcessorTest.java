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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CassandraProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(CassandraProcessorTest.class);
    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    protected static final String TEMPLATES_REPOSITORY_ALIAS_NAME = "paas-template.";
    protected static final String SECRETS_REPOSITORY_ALIAS_NAME = "paas-secrets.";

    @Test@Ignore
    public void creates_structures_and_returns_response() {
        //Given a creation request
        CreateServiceInstanceRequest creationRequest = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                null
        );
        creationRequest.withServiceInstanceId(SERVICE_INSTANCE_ID);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, creationRequest);
        context.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour
        ArgumentCaptor<Path> pathValueCapture = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<String> stringValueCapture = ArgumentCaptor.forClass(String.class);
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        doNothing().when(templatesGenerator).setWorkDir(pathValueCapture.capture());
        doNothing().when(templatesGenerator).setServiceInstanceId(stringValueCapture.capture());
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);
        doNothing().when(secretsGenerator).setWorkDir(pathValueCapture.capture());
        doNothing().when(secretsGenerator).setServiceInstanceId(stringValueCapture.capture());

        //When
        CassandraProcessor cassandraProcessor = new CassandraProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, templatesGenerator, secretsGenerator, null);
        cassandraProcessor.preCreate(context);

        //Then verify parameters and delegation on calls
        List<Path> capturedPaths = pathValueCapture.getAllValues();
        assertEquals(aGitRepoWorkDir().toString(), capturedPaths.get(0).toString());
        assertEquals(aGitRepoWorkDir().toString(), capturedPaths.get(1).toString());
        List<String> capturedStrings = stringValueCapture.getAllValues();
        assertEquals(SERVICE_INSTANCE_ID, capturedStrings.get(0).toString());
        assertEquals(SERVICE_INSTANCE_ID, capturedStrings.get(1).toString());
        verify(templatesGenerator, times(1)).checkPrerequisites();
        verify(templatesGenerator, times(1)).generate();
        verify(secretsGenerator, times(1)).checkPrerequisites();
        verify(secretsGenerator, times(1)).generate();

        //Then verify populated context
        CreateServiceInstanceResponse serviceInstanceResponse = (CreateServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();
        // with a timestamp used to timeout on too long responses
        assertThat(serviceInstanceResponse.getOperation()).isEqualTo(CassandraProcessorConstants.OSB_OPERATION_CREATE);
        // and with a proper commit message
        String customMessage = (String) context.contextKeys.get(GitProcessorContext.commitMessage.toString());
        assertThat(customMessage).isEqualTo("Cassandra broker" + ": "+ CassandraProcessorConstants.OSB_OPERATION_CREATE + " instance id=" + SERVICE_INSTANCE_ID);


    }

    @Test
    public void responds_to_get_last_service_operation_in_progress() {

        //Given a get last operation request (asynchronous polling from Cloud Controller)
        GetLastServiceOperationRequest operationRequest = new GetLastServiceOperationRequest(SERVICE_INSTANCE_ID,
                "service_definition_id",
                "plan_id",
                CassandraProcessorConstants.OSB_OPERATION_CREATE);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withDescription("Creation is in progress");
        expectedResponse.withOperationState(OperationState.IN_PROGRESS);
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(CassandraProcessorConstants.OSB_OPERATION_CREATE))).thenReturn(expectedResponse);


        //When
        CassandraProcessor cassandraProcessor = new CassandraProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, null, tracker);
        cassandraProcessor.preGetLastOperation(context);

        //Then mapped response from tracker is returned
        GetLastServiceOperationResponse operationResponse = (GetLastServiceOperationResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE);
        assertThat(operationResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void responds_to_get_last_service_operation_suceeded() {

        //Given a get last operation request (asynchronous polling from Cloud Controller)
        GetLastServiceOperationRequest operationRequest = new GetLastServiceOperationRequest(SERVICE_INSTANCE_ID,
                "service_definition_id",
                "plan_id",
                CassandraProcessorConstants.OSB_OPERATION_CREATE);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (succeeded state)
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withDescription("Creation is succeeded");
        expectedResponse.withOperationState(OperationState.SUCCEEDED);
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(CassandraProcessorConstants.OSB_OPERATION_CREATE))).thenReturn(expectedResponse);

        //When
        CassandraProcessor cassandraProcessor = new CassandraProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, null, tracker);
        cassandraProcessor.preGetLastOperation(context);

        //Then mapped response from tracker is returned
        GetLastServiceOperationResponse operationResponse = (GetLastServiceOperationResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE);
        assertThat(operationResponse).isEqualTo(expectedResponse);
    }

    @Test@Ignore
    public void removes_secrets_structures_and_returns_response() {
        //Given a delete request
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest(SERVICE_INSTANCE_ID,
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                false);

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour
        ArgumentCaptor<Path> pathValueCapture = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<String> stringValueCapture = ArgumentCaptor.forClass(String.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);
        doNothing().when(secretsGenerator).setWorkDir(pathValueCapture.capture());
        doNothing().when(secretsGenerator).setServiceInstanceId(stringValueCapture.capture());

        //When
        CassandraProcessor cassandraProcessor = new CassandraProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, secretsGenerator, null);
        cassandraProcessor.preDelete(context);

        //Then verify parameters and delegation on calls
        List<Path> capturedPaths = pathValueCapture.getAllValues();
        assertEquals(aGitRepoWorkDir().toString(), capturedPaths.get(0).toString());
        List<String> capturedStrings = stringValueCapture.getAllValues();
        assertEquals(SERVICE_INSTANCE_ID, capturedStrings.get(0).toString());
        verify(secretsGenerator, times(1)).checkPrerequisites();
        verify(secretsGenerator, times(1)).remove();

        //Then verify populated context
        DeleteServiceInstanceResponse serviceInstanceResponse = (DeleteServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isFalse();
        // and with a proper commit message
        String customMessage = (String) context.contextKeys.get(GitProcessorContext.commitMessage.toString());
        assertThat(customMessage).isEqualTo("Cassandra broker" + ": "+ CassandraProcessorConstants.OSB_OPERATION_DELETE + " instance id=" + SERVICE_INSTANCE_ID);

    }



    protected Path aGitRepoWorkDir() {
        return FileSystems.getDefault().getPath("/a/git_workdir/path");
    }

}