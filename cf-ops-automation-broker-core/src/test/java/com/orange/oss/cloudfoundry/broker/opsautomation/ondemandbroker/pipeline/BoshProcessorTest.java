package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceBindingService;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants.ORIGINATING_EMAIL_KEY;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants.ORIGINATING_USER_KEY;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_ORGANIZATION_GUID;
import static com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService.OSB_PROFILE_SPACE_GUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BoshProcessorTest {

    private static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";
    private static final String TEMPLATES_REPOSITORY_ALIAS_NAME = "paas-template.";
    private static final String SECRETS_REPOSITORY_ALIAS_NAME = "paas-secrets.";

    @Test
    public void creates_structures_and_returns_async_response() {
        //Given a creation request
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id1")
                        .spaceGuid("space_id1")
                        .build()
                )
                .build();

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);

        //given a configured timeout
        PipelineCompletionTracker tracker = aCompletionTracker();

        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c_", "_","https://static-dashboard.com");

        //When
        boshProcessor.preCreate(context);

        //Then verify parameters and delegation on calls
        verify(templatesGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(templatesGenerator).generate(eq(aGitRepoWorkDir()), eq(SERVICE_INSTANCE_ID), any(CoabVarsFileDto.class));
        verify(secretsGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(secretsGenerator).generate(aGitRepoWorkDir(), SERVICE_INSTANCE_ID, null);

        //Then verify populated context
        CreateServiceInstanceResponse serviceInstanceResponse = (CreateServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();
        //and specifying dashboard url
        assertThat(serviceInstanceResponse.getDashboardUrl()).isNotNull();


        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(request, "2017-11-14T17:24:08.007Z");
        String expectedJsonPipelineOperationState = tracker.formatAsJson(pipelineOperationState);

        //when
        assertThat(serviceInstanceResponse.getOperation()).isEqualTo(expectedJsonPipelineOperationState);
         // and with a non-null commit message (asserted in a specific test)
        String customTemplateMessage = (String) context.contextKeys.get(TEMPLATES_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customTemplateMessage).isNotNull();
        String customSecretsMessage = (String) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customSecretsMessage).isNotNull();
    }
    @Test
    public void updates_coab_vars_and_returns_async_response() {
        //Given a creation request
        UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id1")
                        .spaceGuid("space_id1")
                        .build()
                )
                .build();

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);

        //given a configured timeout
        PipelineCompletionTracker tracker = aCompletionTracker();

        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c_", "_","https://static-dashboard.com");

        //When
        boshProcessor.preUpdate(context);

        //Then verify parameters and delegation on calls
        verify(templatesGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(templatesGenerator).generateCoabVarsFile(eq(aGitRepoWorkDir()), eq(SERVICE_INSTANCE_ID), any(CoabVarsFileDto.class));

        //Then verify populated context
        UpdateServiceInstanceResponse serviceInstanceResponse =
            (UpdateServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.UPDATE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();
        //and specifying dashboard url
        assertThat(serviceInstanceResponse.getDashboardUrl()).isNotNull();


        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(request, "2017-11-14T17:24:08.007Z");
        String expectedJsonPipelineOperationState = tracker.formatAsJson(pipelineOperationState);

        //when
        assertThat(serviceInstanceResponse.getOperation()).isEqualTo(expectedJsonPipelineOperationState);
         // and with a non-null commit message (asserted in a specific test)
        String customTemplateMessage = (String) context.contextKeys.get(TEMPLATES_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customTemplateMessage).isNotNull();
        String customSecretsMessage = (String) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME+GitProcessorContext.commitMessage.toString());
        assertThat(customSecretsMessage).isNotNull();
    }

    @Test
    public void constructs_a_dto_from_a_provisionning_request() {
        //Given mocked dependencies
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);
        PipelineCompletionTracker tracker = aCompletionTracker();

        //Given a basic processor with deployment model
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c", "_","https://static-dashboard.com");


        //Given a creation request with both deprecated and new OSB syntax
        Map<String, Object> contextProperties = new HashMap<>();
        contextProperties.put(OSB_PROFILE_ORGANIZATION_GUID, "org_id1");
        contextProperties.put(OSB_PROFILE_SPACE_GUID, "space_id1");

        Map<String, Object> params = new HashMap<>();
        params.put("a-string-param", "a-string-value");
        params.put("a number param", 24);
        params.put("a boolean param", true);

        //Given a creation request
        Map<String, Object> properties = new HashMap<>();
        properties.put(ORIGINATING_USER_KEY, "user_guid1");
        properties.put(ORIGINATING_EMAIL_KEY, "user_email");
        org.springframework.cloud.servicebroker.model.Context identityContext = CloudFoundryContext.builder()
                .properties(properties).build();
        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id1")
                .planId("plan_id1")
                .serviceInstanceId("service-instance-id1")
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id1")
                        .spaceGuid("space_id1")
                        .build()
                )
                .parameters(params)
                .originatingIdentity(identityContext)
                .build();

        //when
        CoabVarsFileDto coabVarsFileDto = boshProcessor.wrapCreateOsbIntoVarsDto(request);

        //then
        assertThat(coabVarsFileDto.instance_id).isEqualTo("service-instance-id1");
        assertThat(coabVarsFileDto.plan_id).isEqualTo("plan_id1");
        assertThat(coabVarsFileDto.service_id).isEqualTo("service_definition_id1");
        assertThat(coabVarsFileDto.deployment_name).isEqualTo("c" + "_" + "service-instance-id1");
        assertThat(coabVarsFileDto.context.organization_guid).isEqualTo("org_id1");
        assertThat(coabVarsFileDto.context.space_guid).isEqualTo("space_id1");
        assertThat(coabVarsFileDto.context.user_guid).isEqualTo("user_guid1");
        assertThat(coabVarsFileDto.parameters).isEqualTo(params);
    }

    @Test
    public void formats_dashboard_url_when_configured_with_template() {
        formats_dashboard_url_when_configured(
                "https://shield_{0}.redacted-ops-domain.com",
                "https://shield_service-instance-id1.redacted-ops-domain.com");
        formats_dashboard_url_when_configured(
                "https://shield_{1}.redacted-ops-domain.com",
                "https://shield_brokered-service-instance-id2.redacted-ops-domain.com");
    }

    @Test
    public void formats_dashboard_url_when_configured_with_template_and_cmdb_params_missing() {
        formats_dashboard_url_when_configured("https://shield_{0}.redacted-ops-domain.com",
            "https://shield_service-instance-id1.redacted-ops-domain.com",
            aCreateServiceInstanceRequestWithoutOsbCmdbParam());
        formats_dashboard_url_when_configured("https://shield_{1}.redacted-ops-domain.com",
            "https://shield_null.redacted-ops-domain.com",
            aCreateServiceInstanceRequestWithoutOsbCmdbParam());
        formats_dashboard_url_when_configured("https://shield_{1}.redacted-ops-domain.com",
            "https://shield_null.redacted-ops-domain.com",
            aCreateServiceInstanceRequestWithPartialOsbCmdbParam());
    }

    @Test
    public void formats_dashboard_url_when_configured_with_static() {
        formats_dashboard_url_when_configured(
                "https://static.redacted-ops-domain.com",
                "https://static.redacted-ops-domain.com");
    }

    @Test
    public void formats_no_dashboard_url_when_not_configured() {
        formats_dashboard_url_when_configured(
                null,
                null);
    }

    protected void formats_dashboard_url_when_configured(String dashboardUrlTemplate, String expected) {
        formats_dashboard_url_when_configured(dashboardUrlTemplate, expected,
            aCreateServiceInstanceRequestWithOsbCmdbParam());
    }

    private void formats_dashboard_url_when_configured(String dashboardUrlTemplate, String expected,
        CreateServiceInstanceRequest request) {
        //given
        BoshProcessor boshProcessor = aBasicBoshProcessor();

        //When
        String dashboardUrl = boshProcessor.formatDashboardOnCreate(dashboardUrlTemplate, request);
        //then
        assertThat(dashboardUrl).isEqualTo(expected);
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequestWithOsbCmdbParam() {
        String brokeredServiceGuid = "brokered-service-instance-id2";
        Map<String, Object> parameters = OsbBuilderHelper.osbCmdbCustomParam(brokeredServiceGuid);
        return CreateServiceInstanceRequest.builder()
            .serviceInstanceId("service-instance-id1")
            .parameters(parameters)
            .build();
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequestWithPartialOsbCmdbParam() {
        Map<String, Map<String,String>> osbCmdbMetaData = new HashMap<>();
        osbCmdbMetaData.put(BoshProcessor.CMDB_LABELS_KEY,
            Collections.singletonMap("a-random-key",
                "brokered-service-instance-id2"));
        return CreateServiceInstanceRequest.builder()
            .serviceInstanceId("service-instance-id1")
            .parameters(Collections.singletonMap(BoshProcessor.X_OSB_CMDB_CUSTOM_KEY_NAME, osbCmdbMetaData))
            .build();
    }

    private CreateServiceInstanceRequest aCreateServiceInstanceRequestWithoutOsbCmdbParam() {
        return CreateServiceInstanceRequest.builder()
            .serviceInstanceId("service-instance-id1")
            .build();
    }


    @Test
    public void provisions_commit_msg_including_requester_details_with_empty_context() {
        //Given a creation request with both deprecated OSB syntax
        Map<String, Object> properties = new HashMap<>();
        org.springframework.cloud.servicebroker.model.Context context = CloudFoundryContext.builder().build();

        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id1")
                .planId("plan_id1")
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id1")
                        .spaceGuid("space_id1")
                        .build()
                )
                .originatingIdentity(context)
                .build();


        //When
        //then commit msg is valid
        BoshProcessor boshProcessor = aBasicBoshProcessor();
        assertThat(boshProcessor.formatProvisionCommitMsg(request)).isEqualTo("Cassandra broker: create instance id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0" +
                "\n\nRequested from space_guid=space_id1 org_guid=org_id1 by user_guid=null");
    }

    @Test
    public void provisions_commit_msg_including_requester_details_without_context() {
        //Given a creation request with both deprecated OSB syntax

        //Given a parameter request
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameterName", "parameterValue");

        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .parameters(parameters)
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id1")
                        .spaceGuid("space_id1")
                        .build()
                )
                .build();

        //then commit msg is valid
        BoshProcessor boshProcessor = aBasicBoshProcessor();
        assertThat(boshProcessor.formatProvisionCommitMsg(request)).isEqualTo("Cassandra broker: create instance id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0" +
                "\n\nRequested from space_guid=space_id1 org_guid=org_id1 by user_guid=null");
    }

    @Test
    public void provisions_commit_msg_including_requester_details_with_context() {
        //Given a creation request with both deprecated OSB syntax and new context syntax
        Map<String, Object> properties = new HashMap<>();
        properties.put(ORIGINATING_USER_KEY, "user_guid1");
        org.springframework.cloud.servicebroker.model.Context context = CloudFoundryContext.builder()
                .properties(properties).build();

        CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .context(CloudFoundryContext.builder()
                        .organizationGuid("org_id1")
                        .spaceGuid("space_id1")
                        .build()
                )
                .originatingIdentity(context)
                .build();

        provisions_commit_msg_including_requester_details(request);
    }

    protected void provisions_commit_msg_including_requester_details(CreateServiceInstanceRequest request) {
        BoshProcessor boshProcessor = aBasicBoshProcessor();

        //When
        assertThat(boshProcessor.formatProvisionCommitMsg(request)).isEqualTo("Cassandra broker: create instance id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0" +
                "\n\nRequested from space_guid=space_id1 org_guid=org_id1 by user_guid=user_guid1");
    }

    private BoshProcessor aBasicBoshProcessor() {
        //Given mocked dependencies
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);
        PipelineCompletionTracker tracker = aCompletionTracker();

        return new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c", "_","https://static-dashboard.com");
    }

    @Test
    public void unprovision_commit_msg_includes_requester_details() {
        //Given a delete request
        // Given an incoming delete request
        Map<String, Object> properties = new HashMap<>();
        properties.put(ORIGINATING_USER_KEY, "user_guid1");
        org.springframework.cloud.servicebroker.model.Context context = CloudFoundryContext.builder()
                .properties(properties).build();

        DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .serviceDefinition(ServiceDefinition.builder().build())
                .asyncAccepted(false)
                .originatingIdentity(context)
                .build();



        //Given mocked dependencies
        BoshProcessor boshProcessor = aBasicBoshProcessor();

        //When
        assertThat(boshProcessor.formatUnprovisionCommitMsg(request)).isEqualTo("Cassandra broker: delete instance id=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0" +
                "\n\nRequested by user_guid=user_guid1");
    }


    @Test
    public void responds_to_get_last_service_operation_in_progress() {

        //Given a get last operation request (asynchronous polling from Cloud Controller)
        GetLastServiceOperationRequest operationRequest = GetLastServiceOperationRequest.builder()
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .operation(DeploymentConstants.OSB_OPERATION_CREATE)
                .build();

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        GetLastServiceOperationResponse expectedResponse = GetLastServiceOperationResponse.builder()
            .operationState(OperationState.IN_PROGRESS)
            .build();
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(DeploymentConstants.OSB_OPERATION_CREATE), any(GetLastServiceOperationRequest.class))).thenReturn(expectedResponse);


        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c", "_","https://static-dashboard.com");
        boshProcessor.preGetLastOperation(context);

        //Then mapped response from tracker is returned
        GetLastServiceOperationResponse operationResponse = (GetLastServiceOperationResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE);
        assertThat(operationResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void delegates_bind_request_to_completion_nested_broker() {

        //given
        CreateServiceInstanceBindingRequest request = OsbBuilderHelper.aBindingRequest("service-instance-id");

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_REQUEST, request);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.delegateBindRequest(any(Path.class), any(CreateServiceInstanceBindingRequest.class))).thenReturn(OsbBuilderHelper.aBindingResponse());


        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c", "_","https://static-dashboard.com");
        boshProcessor.preBind(context);

        //Then
        verify(tracker).delegateBindRequest(any(Path.class), eq(request));
        //And mapped response from tracker is returned
        CreateServiceInstanceBindingResponse bindingResponse = (CreateServiceInstanceBindingResponse) context.contextKeys.get(ProcessorChainServiceInstanceBindingService.CREATE_SERVICE_INSTANCE_BINDING_RESPONSE);
        assertThat(bindingResponse).isEqualTo(OsbBuilderHelper.aBindingResponse());
    }

    @Test
    public void delegates_unbind_request_to_completion_nested_broker() {
        //given
        DeleteServiceInstanceBindingRequest request = OsbBuilderHelper.anUnbindRequest("service-instance-id", "service-binding-id");

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceBindingService.DELETE_SERVICE_INSTANCE_BINDING_REQUEST, request);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (in progress state)
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        doNothing().when(tracker).delegateUnbindRequest(any(Path.class), any(DeleteServiceInstanceBindingRequest.class));

        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c", "_","https://static-dashboard.com");
        boshProcessor.preUnBind(context);

        //Then
        verify(tracker).delegateUnbindRequest(any(Path.class), eq(request));
    }


    @Test
    public void responds_to_get_last_service_operation_succeeded() {

        //Given a get last operation request (asynchronous polling from Cloud Controller)
        GetLastServiceOperationRequest operationRequest = GetLastServiceOperationRequest.builder()
        .serviceInstanceId(SERVICE_INSTANCE_ID)
                .serviceDefinitionId("service_definition_id")
                .planId("plan_id")
                .operation(DeploymentConstants.OSB_OPERATION_CREATE)
                .build();

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());

        //Given a mock behaviour (succeeded state)
        GetLastServiceOperationResponse expectedResponse = GetLastServiceOperationResponse.builder()
                .description("Creation is succeeded")
                .operationState(OperationState.SUCCEEDED)
                .build();
        PipelineCompletionTracker tracker = mock(PipelineCompletionTracker.class);
        when(tracker.getDeploymentExecStatus(any(Path.class), eq(SERVICE_INSTANCE_ID), eq(DeploymentConstants.OSB_OPERATION_CREATE), any(GetLastServiceOperationRequest.class))).thenReturn(expectedResponse);

        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, null, null, tracker, "Cassandra", "c", "_","https://static-dashboard.com");
        boshProcessor.preGetLastOperation(context);

        //Then mapped response from tracker is returned
        GetLastServiceOperationResponse operationResponse = (GetLastServiceOperationResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE);
        assertThat(operationResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void unprovision_removes_secrets_structures_and_returns_async_response() {
        //Given a delete request
        // Given an incoming delete request
        DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
                .serviceInstanceId(SERVICE_INSTANCE_ID)
                .serviceDefinitionId("service_id")
                .planId("plan_id")
                .serviceDefinition(ServiceDefinition.builder().build())
                .asyncAccepted(false)
                .originatingIdentity(aContext())
                .build();

        //Given a populated context
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());
        context.contextKeys.put(TEMPLATES_REPOSITORY_ALIAS_NAME + GitProcessorContext.workDir.toString(), aGitRepoWorkDir());


        //Given a mock behaviour
        SecretsGenerator secretsGenerator = mock(SecretsGenerator.class);
        TemplatesGenerator templatesGenerator = mock(TemplatesGenerator.class);

        //given a configured timeout within tracker
        PipelineCompletionTracker tracker = aCompletionTracker();


        //When
        BoshProcessor boshProcessor = new BoshProcessor(TEMPLATES_REPOSITORY_ALIAS_NAME, SECRETS_REPOSITORY_ALIAS_NAME, templatesGenerator, secretsGenerator, tracker, "Cassandra", "c", "_","https://static-dashboard.com");
        boshProcessor.preDelete(context);

        //Then verify parameters and delegation on calls
        verify(secretsGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(secretsGenerator).remove(aGitRepoWorkDir(), SERVICE_INSTANCE_ID);

        verify(templatesGenerator).checkPrerequisites(aGitRepoWorkDir());
        verify(templatesGenerator).remove(aGitRepoWorkDir(), SERVICE_INSTANCE_ID);

        //Then verify populated context
        DeleteServiceInstanceResponse serviceInstanceResponse = (DeleteServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();

        //and operation state is specified
        PipelineCompletionTracker.PipelineOperationState pipelineOperationState = new PipelineCompletionTracker.PipelineOperationState(request, "2017-11-14T17:24:08.007Z");
        String expectedJsonPipelineOperationState = tracker.formatAsJson(pipelineOperationState);

        assertThat(serviceInstanceResponse.getOperation()).isEqualTo(expectedJsonPipelineOperationState);

        // and with a commit message (asserted in a distinct test)
        String customMessage = (String) context.contextKeys.get(SECRETS_REPOSITORY_ALIAS_NAME + GitProcessorContext.commitMessage.toString());
        assertThat(customMessage).isNotNull();
    }



    private PipelineCompletionTracker aCompletionTracker() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        return new PipelineCompletionTracker(clock, 1200L, Mockito.mock(OsbProxy.class), Mockito.mock(SecretsReader.class));
    }

    private Path aGitRepoWorkDir() {
        return FileSystems.getDefault().getPath("/a/git_workdir/path");
    }

    private org.springframework.cloud.servicebroker.model.Context aContext() {
        return OsbBuilderHelper.aCfUserContext();
    }


}
