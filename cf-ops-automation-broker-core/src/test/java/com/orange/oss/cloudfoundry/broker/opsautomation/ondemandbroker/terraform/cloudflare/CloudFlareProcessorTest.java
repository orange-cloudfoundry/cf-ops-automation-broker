package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudFlareProcessorTest {

    @Mock
    TerraformRepository terraformRepository;

    @InjectMocks
    CloudFlareProcessor cloudFlareProcessor = new CloudFlareProcessor(aConfig(), terraformRepository);


    @Test
    public void accepts_correct_requested_routes() {
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), terraformRepository);

        //given a user performing
        //cf cs cloudflare -c '{route="a-valid-route"}'
        Context context = aContextWithCreateRequest("route", "a-valid-route");

        //when the processor is invoked
        cloudFlareProcessor.preCreate(context);

        //then no exception is thrown
    }

    @Test
    public void rejects_invalid_requested_routes() {
        //Given an invalid route
        Context context = aContextWithCreateRequest("route", "@");

        try {
            cloudFlareProcessor.preCreate(context);
            Assert.fail("expected to be rejected");
        } catch (RuntimeException e) {
            //Message should indicate to end user the incorrect param name and value
            assertThat(e.getMessage()).contains("route");
            assertThat(e.getMessage()).contains("@");
        }
    }

    @Test
    public void rejects_duplicate_route_request() {
        //given a repository populated with an existing module
        TerraformRepository terraformRepository = Mockito.mock(TerraformRepository.class);
        when(terraformRepository.getByModuleProperty("route-prefix", "avalidroute")).thenReturn(aTfModule());
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), terraformRepository);

        //When a new module is requested to be added
        TerraformModule requestedModule = ImmutableTerraformModule.builder().from(aTfModule())
                .id("service-instance-guid")
                .moduleName("cloudflare-route-ondemandroute5")
                .putProperties("route-prefix", "avalidroute")
                .build();


        //Then it checks if a conflicting module exists, and rejects the request
        try {
            cloudFlareProcessor.checkForConflictingProperty(requestedModule, "route-prefix", "route");
        } catch (RuntimeException e) {
            //Message should indicate to end user the incorrect param name and value
            assertThat(e.getMessage()).contains("route");
            assertThat(e.getMessage()).contains("avalidroute");
        }
    }

    @Test(expected = RuntimeException.class)
    public void prevents_modules_submission_with_conflicting_module_name() {
        //given a repository populated with an existing module
        TerraformRepository terraformRepository = Mockito.mock(TerraformRepository.class);
        when(terraformRepository.getByModuleName("cloudflare-route-ondemandroute5")).thenReturn(aTfModule());
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), terraformRepository);

        //When a new module is requested to be added
        // by a previous processor in the chain that inserted a tf module in the context
        TerraformModule requestedModule = ImmutableTerraformModule.builder().from(aTfModule())
                .id("service-instance-guid")
                .moduleName("cloudflare-route-ondemandroute5")
                .build();


        //Then it checks if a conflicting module exists, and rejects the request
        //when
        cloudFlareProcessor.checkForConflictingModuleName(requestedModule);
    }

    @Test(expected = RuntimeException.class)
    public void prevents_modules_submission_with_conflicting_module_id() {
        //given a repository populated with an existing module
        TerraformRepository terraformRepository = Mockito.mock(TerraformRepository.class);
        when(terraformRepository.getByModuleId("service-instance-guid")).thenReturn(aTfModule());
        cloudFlareProcessor = new CloudFlareProcessor(aConfig(), terraformRepository);

        //When a new module is requested to be added
        // by a previous processor in the chain that inserted a tf module in the context
        TerraformModule requestedModule = ImmutableTerraformModule.builder().from(aTfModule())
                .id("service-instance-guid")
                .build();


        //Then it checks if a conflicting module exists, and rejects the request
        //when
        cloudFlareProcessor.checkForConflictingModuleId(requestedModule);
    }


    @Test
    public void looks_up_paas_secrets_git_local_checkout() throws IOException {
        //given
        //the git mediation cloned the repo and populated context
        Context ctx = new Context();
        Path workDir = Files.createTempDirectory("paas-secret-clone");

        ctx.contextKeys.put(GitProcessorContext.workDir.toString(),workDir);

        //when
        //Path lookedUpWorkDir = terraformModuleProcessor.lookUpGitworkDir(ctx);

        //then
        //Asserts.assertThat(lookedUpWorkDir.equals());

    }


    @Test
    public void creates_tf_module() {
        //given a tf module template available in the classpath
        TerraformModule deserialized = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
        ImmutableCloudFlareConfig cloudFlareConfig = ImmutableCloudFlareConfig.builder()
                .routeSuffix("-cdn-cw-vdr-pprod-apps.redacted-domain.org")
                .template(deserialized).build();
        cloudFlareProcessor = new CloudFlareProcessor(cloudFlareConfig, terraformRepository);

        //given a user request with a route
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("route", "avalidroute");
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
                .id("serviceinstance_guid")
                .moduleName("serviceinstance_guid")
                .putProperties("org_guid", "org_id")
                .putProperties("route-prefix", "avalidroute")
                .putProperties("service_instance_guid", "serviceinstance_guid")
                .putProperties("space_guid", "space_id")
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
       TerraformModule deserialized = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
       ImmutableCloudFlareConfig cloudFlareConfig = ImmutableCloudFlareConfig.builder()
               .routeSuffix("-cdn-cw-vdr-pprod-apps.redacted-domain.org")
               .template(deserialized).build();
       cloudFlareProcessor = new CloudFlareProcessor(cloudFlareConfig, terraformRepository);

       //given a user request with a route
       Map<String, Object> parameters = new HashMap<>();
       parameters.put("route", "avalidroute");
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

       //when
       cloudFlareProcessor.preCreate(context);

       //then it injects a terraform module into the repository
       verify(terraformRepository).save(any(TerraformModule.class));

       //and populates a response into the context
       CreateServiceInstanceResponse serviceInstanceResponse = (CreateServiceInstanceResponse) context.contextKeys.get(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_RESPONSE);
       assertThat(serviceInstanceResponse.isAsync()).isTrue();
   }


    Context aContextWithCreateRequest() {
        return aContextWithCreateRequest("route", "a-valid-route");
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

    private CloudFlareConfig aConfig() {
        TerraformModule template = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
        return ImmutableCloudFlareConfig.builder()
                .template(template)
                .routeSuffix("-cdn-cw-vdr-pprod-apps.redacted-domain.org").build();
    }

    private ImmutableTerraformModule aTfModule() {
        return ImmutableTerraformModule.builder()
                .moduleName("module1")
                .source("path/to/module").build();
    }


}
