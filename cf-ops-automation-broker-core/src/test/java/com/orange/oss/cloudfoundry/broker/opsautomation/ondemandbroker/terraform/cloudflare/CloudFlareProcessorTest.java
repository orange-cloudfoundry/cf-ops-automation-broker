package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.*;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class CloudFlareProcessorTest {

    CloudFlareProcessor cloudFlareProcessor = new CloudFlareProcessor(aConfig());


    @Test
    public void accepts_correct_requested_routes() {
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
    public void injects_tf_module_into_context() {
        //given a tf module template available in the classpath
        TerraformModule deserialized = TerraformModuleHelper.getTerraformModuleFromClasspath("/terraform/cloudflare-module-template.tf.json");
        ImmutableCloudFlareConfig cloudFlareConfig = ImmutableCloudFlareConfig.builder()
                .routeSuffix("-cdn-cw-vdr-pprod-apps.redacted-domain.org")
                .template(deserialized).build();
        cloudFlareProcessor = new CloudFlareProcessor(cloudFlareConfig);

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

        //then it injects a terraform module into the context
        TerraformModule terraformModule = (TerraformModule) context.contextKeys.get(TerraformModuleProcessor.ADD_TF_MODULE_WITH_ID);

        ImmutableTerraformModule expectedModule = ImmutableTerraformModule.builder().from(deserialized)
                .moduleName(request.getServiceInstanceId())
                .putProperties("org_guid", "org_id")
                .putProperties("route-prefix", "avalidroute")
                .putProperties("service_instance_guid", "serviceinstance_guid")
                .putProperties("space_guid", "space_id")
                .build();

        assertThat(terraformModule).isEqualTo(expectedModule);
    }


    @Test
    public void rejects_duplicate_route_request() {

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


}
