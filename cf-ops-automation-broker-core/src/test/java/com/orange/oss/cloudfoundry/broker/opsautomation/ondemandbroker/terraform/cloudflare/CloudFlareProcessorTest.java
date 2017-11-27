package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.servicebroker.model.OperationState.IN_PROGRESS;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudFlareProcessorTest {

    @Mock
    TerraformRepository terraformRepository;

    CloudFlareProcessor cloudFlareProcessor = new CloudFlareProcessor(aConfig(), aSuffixValidator(), getRepositoryFactory(), aTracker());


    @Test(expected = UnsupportedOperationException.class)
    public void rejects_bind_calls() {
        cloudFlareProcessor.preBind(new Context());
    }

    @Test
    public void rejects_invalid_requested_routes() {
        //given a user performing
        //cf cs cloudflare -c '{route="@"}'
        Context context = aContextWithCreateRequest(CloudFlareProcessor.ROUTE_PREFIX, "@");

        try {
            cloudFlareProcessor.preCreate(context);
            Assert.fail("expected to be rejected");
        } catch (RuntimeException e) {
            //Message should indicate to end user the incorrect param name and value
            assertThat(e.getMessage()).contains(CloudFlareProcessor.ROUTE_PREFIX);
            assertThat(e.getMessage()).contains("@");
        }
    }

    @Test
    public void rejects_duplicate_route_request() {
        //given a repository populated with an existing module
        TerraformRepository terraformRepository = Mockito.mock(TerraformRepository.class);
        when(terraformRepository.getByModuleProperty(CloudFlareProcessor.ROUTE_PREFIX, "avalidroute")).thenReturn(aTfModule());

        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), aSuffixValidator(), getRepositoryFactory(), aTracker());

        //When a new module is requested to be added
        TerraformModule requestedModule = ImmutableTerraformModule.builder().from(aTfModule())
                .moduleName("service-instance-guid")
                .putProperties(CloudFlareProcessor.ROUTE_PREFIX, "avalidroute")
                .build();


        //Then it checks if a conflicting module exists, and rejects the request
        try {
            cloudFlareProcessor.checkForConflictingProperty(requestedModule, CloudFlareProcessor.ROUTE_PREFIX, CloudFlareProcessor.ROUTE_PREFIX, terraformRepository);
            Assert.fail("expected to be rejected");
        } catch (RuntimeException e) {
            //Message should indicate to end user the incorrect param name and value
            assertThat(e.getMessage()).contains(CloudFlareProcessor.ROUTE_PREFIX);
            assertThat(e.getMessage()).contains("avalidroute");
        }
    }

    @Test
    public void prevents_modules_submission_with_conflicting_module_name() {
        //given a repository populated with an existing module
        TerraformRepository terraformRepository = Mockito.mock(TerraformRepository.class);
        ImmutableTerraformModule aTfModule = ImmutableTerraformModule.builder()
                .source("path/to/module")
                .moduleName("service-instance-guid")
                .build();
        when(terraformRepository.getByModuleName("service-instance-guid")).thenReturn(aTfModule);
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), aSuffixValidator(), getRepositoryFactory(), null);

        //When a new module is requested to be added
        // by a previous processor in the chain that inserted a tf module in the context
        try {
            cloudFlareProcessor.checkForConflictingModuleName(aTfModule, terraformRepository);
            Assert.fail("expected to be rejected");
        } catch (RuntimeException e) {
            //Then it checks if a conflicting module exists, and rejects the request
            assertThat(e.getMessage()).containsIgnoringCase("conflict");
        }
    }

    @Test
    public void looks_up_paas_secrets_git_local_checkout() throws IOException {
        //given
        //the git mediation cloned the repo and populated context
        Context ctx = new Context();
        Path workDir = Files.createTempDirectory("paas-secret-clone");
        ctx.contextKeys.put(GitProcessorContext.workDir.toString(),workDir);

        //and a repository factory instianciating file repository

        TerraformRepository.Factory repositoryFactory = FileTerraformRepository.getFactory("cloudflare-");
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), aSuffixValidator(), repositoryFactory, null);

        //when
        FileTerraformRepository repository = (FileTerraformRepository) cloudFlareProcessor.getRepository(ctx);
        //Path lookedUpWorkDir = terraformModuleProcessor.lookUpGitworkDir(ctx);

        //then
        assertThat(repository.getDirectory().toFile()).isEqualTo(workDir.toFile());
    }

    @Test(expected = RuntimeException.class)
    public void missing_paas_secrets_git_local_checkout_triggers_OSB_retries() {
        //given the git mediation failed to properly clone the repo
        Context ctx = new Context();

        TerraformRepository.Factory repositoryFactory = FileTerraformRepository.getFactory("cloudflare-");
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), aSuffixValidator(), repositoryFactory, null);

        //when
        FileTerraformRepository repository = (FileTerraformRepository) cloudFlareProcessor.getRepository(ctx);

        //then OSB will retry polling status, or ask user to retry the delete request.
    }


    @Test
    public void creates_tf_module() {
        //given a tf module template available in the classpath
        TerraformModule deserialized = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
        ImmutableCloudFlareConfig cloudFlareConfig = ImmutableCloudFlareConfig.builder()
                .routeSuffix("-cdn-cw-vdr-pprod-apps.redacted-domain.org")
                .template(deserialized).build();
        cloudFlareProcessor = new CloudFlareProcessor(cloudFlareConfig, aSuffixValidator(), getRepositoryFactory(), aTracker());

        //given a user request with a route
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(CloudFlareProcessor.ROUTE_PREFIX, "avalidroute");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );
        request.withServiceInstanceId("serviceinstance_guid");

        //when
        ImmutableTerraformModule terraformModule = cloudFlareProcessor.constructModule(request);

        //then module is properly formed
        ImmutableTerraformModule expectedModule = ImmutableTerraformModule.builder().from(deserialized)
                .moduleName("serviceinstance_guid")
                .putProperties("org_guid", "org_id")
                .putProperties(CloudFlareProcessor.ROUTE_PREFIX, "avalidroute")
                .putProperties("service_instance_guid", "serviceinstance_guid")
                .putProperties("space_guid", "space_id")
                .outputs(new HashMap<>())
                .putOutputs(
                        "serviceinstance_guid.started",
                        ImmutableOutputConfig.builder().value("${module.serviceinstance_guid.started}").build())
                .putOutputs(
                        "serviceinstance_guid.completed",
                        ImmutableOutputConfig.builder().value("${module.serviceinstance_guid.completed}").build())

                .build();

        assertThat(terraformModule).isEqualTo(expectedModule);
    }

    @Test
    public void creates_tf_module_and_persists_into_repository_and_returns_resp() {
        //given a tf module template available in the classpath
        TerraformModule template = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
        ImmutableCloudFlareConfig cloudFlareConfig = ImmutableCloudFlareConfig.builder()
                .routeSuffix("-cdn-cw-vdr-pprod-apps.redacted-domain.org")
                .template(template).build();

        //given a tf state with no completed execution
        String tfStateFileInClasspath = "/terraform/terraform-without-successfull-module-exec.tfstate";
        //given a configured timeout
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(clock, 120, "terraform.tfstate");

        cloudFlareProcessor = new CloudFlareProcessor(cloudFlareConfig, aSuffixValidator(), getRepositoryFactory(), tracker);


        //given a user request with a route
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(CloudFlareProcessor.ROUTE_PREFIX, "avalidroute");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );
        request.withServiceInstanceId("serviceinstance_guid");

        //and the context being injected to a cloudflare processor
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(GitProcessorContext.workDir.toString(), aGitRepoWorkDir());


        //when
        cloudFlareProcessor.preCreate(context);

        //then it injects a terraform module into the repository
        verify(terraformRepository).save(any(TerraformModule.class));

        //and populates a response into the context
        CreateServiceInstanceResponse serviceInstanceResponse = (CreateServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE);
        // specifying asynchronous creations
        assertThat(serviceInstanceResponse.isAsync()).isTrue();
        // with a timestamp used to timeout on too long responses
        assertThat(serviceInstanceResponse.getOperation()).isEqualTo("2017-11-14T17:24:08.007Z");
        // and with a proper commit message
        String customMessage = (String) context.contextKeys.get(GitProcessorContext.commitMessage.toString());
        assertThat(customMessage).isEqualTo("cloudflare broker: create instance id=serviceinstance_guid with route-prefix=avalidroute");
    }

    @Test
    public void responds_to_get_last_create_service_operation() {
        //Given a tf state without completed module outputs
        TerraformCompletionTracker tracker = Mockito.mock(TerraformCompletionTracker.class);
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withDescription("module exec in progress");
        expectedResponse.withOperationState(IN_PROGRESS);
        when(tracker.getModuleExecStatus(any(Path.class), eq("serviceinstance_guid"), eq("2017-11-14T17:24:08.007Z"))).thenReturn(expectedResponse);

        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), aSuffixValidator(), getRepositoryFactory(), tracker);
        //given an async polling from CC
        GetLastServiceOperationRequest operationRequest = new GetLastServiceOperationRequest("serviceinstance_guid",
                "service_definition_id",
                "plan_id",
                "2017-11-14T17:24:08.007Z");

        //and the context being injected to a cloudflare processor
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_REQUEST, operationRequest);
        context.contextKeys.put(GitProcessorContext.workDir.toString(), aGitRepoWorkDir());


        //when
        cloudFlareProcessor.preGetLastCreateOperation(context);

        // then mapped response from tracker is returned
        GetLastServiceOperationResponse operationResponse = (GetLastServiceOperationResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.GET_LAST_SERVICE_OPERATION_RESPONSE);
        assertThat(operationResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void delete_modules_when_requested() {
        //given a repository with an existing module
        terraformRepository = Mockito.mock(TerraformRepository.class);

        ImmutableTerraformModule aTfModule = ImmutableTerraformModule.builder()
                .from(aTfModule())
                .putProperties(CloudFlareProcessor.ROUTE_PREFIX, "avalidroute")
                .build();
        when(terraformRepository.getByModuleName("instance_id")).thenReturn(aTfModule);
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), aSuffixValidator(), getRepositoryFactory(), aTracker());


        //given an incoming delete request
        DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_REQUEST, request);
        context.contextKeys.put(GitProcessorContext.workDir.toString(), aGitRepoWorkDir());


        //when
        cloudFlareProcessor.preDelete(context);

        //then it deletes the terraform module from the repository
        verify(terraformRepository).delete(aTfModule);

        // and the delete response is returned
        DeleteServiceInstanceResponse response = (DeleteServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.DELETE_SERVICE_INSTANCE_RESPONSE);
        assertThat(response.isAsync()).isFalse();
        // with a proper commit message
        String customMessage = (String) context.contextKeys.get(GitProcessorContext.commitMessage.toString());
        assertThat(customMessage).isEqualTo("cloudflare broker: delete instance id=instance_id with route-prefix=avalidroute");
    }

    protected Path aGitRepoWorkDir() {
        return FileSystems.getDefault().getPath("/a/git_workdir/path");
    }

    TerraformCompletionTracker aTracker() {
        GetLastServiceOperationResponse expectedResponse = new GetLastServiceOperationResponse();
        expectedResponse.withDescription("module exec in progress");
        expectedResponse.withOperationState(IN_PROGRESS);
        TerraformCompletionTracker tracker = Mockito.mock(TerraformCompletionTracker.class);
        when(tracker.getModuleExecStatus(any(Path.class), anyString(), anyString())).thenReturn(expectedResponse);
        when(tracker.getCurrentDate()).thenReturn("2017-11-14T17:24:08.007Z");
        return tracker;
    }

    CloudFlareRouteSuffixValidator aSuffixValidator() {
        return new CloudFlareRouteSuffixValidator(aConfig().getRouteSuffix());
    }

    @SuppressWarnings("SameParameterValue")
    Context aContextWithCreateRequest(String key, String value) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(key, value);
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );
        request.withServiceInstanceId("service-instance-guid");

        //and the context being injected to a cloudflare processor
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);
        return context;
    }

    public static CloudFlareConfig aConfig() {
        TerraformModule template = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
        return ImmutableCloudFlareConfig.builder()
                .template(template)
                .routeSuffix("-cdn-cw-vdr-pprod-apps.redacted-domain.org").build();
    }

    public static ImmutableTerraformModule aTfModule() {
        return ImmutableTerraformModule.builder()
                .source("path/to/module")
                .moduleName("instance_id")
                .build();
    }


    private TerraformRepository.Factory getRepositoryFactory() {
        return path -> this.terraformRepository;
    }

}
