package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.springframework.cloud.servicebroker.model.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants.*;
import static java.util.Arrays.asList;
import static org.springframework.cloud.servicebroker.model.CloudFoundryContext.CLOUD_FOUNDRY_PLATFORM;

public class OsbBuilderHelper {
    static DeleteServiceInstanceBindingRequest anUnbindRequest(String serviceInstanceId) {
        ServiceDefinition serviceDefinition = aCatalog().getServiceDefinitions().get(0);
        DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest(serviceInstanceId, "service-binding-id","coab-serviceid", "coab-planid", serviceDefinition);
        request.withApiInfoLocation("api-info");
        request.withOriginatingIdentity(aContext());
        request.withCfInstanceId("cf-instance-id");
        return request;
    }

    private static Catalog aCatalog() {
        Plan plan = new Plan("plan_id", "plan_name", "plan_description", new HashMap<>());
        Plan plan2 = new Plan("plan_id2", "plan_name2", "plan_description2", new HashMap<>());
        ServiceDefinition serviceDefinition = new ServiceDefinition("service_id", "service_name", "service_description", true, asList(plan, plan2));
        Plan plan3 = new Plan("plan_id3", "plan_name3", "plan_description3", new HashMap<>());
        ServiceDefinition serviceDefinition2 = new ServiceDefinition("service_id2", "service_name2", "service_description3", true, Collections.singletonList(plan3));
        return new Catalog(Collections.singletonList(serviceDefinition));
    }

    public static CreateServiceInstanceBindingRequest aBindingRequest(String serviceInstanceId) {
        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = new BindResource("app_guid", null, routeBindingParams);

        Map<String, Object> cfContextProps = new HashMap<>();
        cfContextProps.put("user_id", "a_user_guid");
        cfContextProps.put("organization_guid", "org_guid");
        cfContextProps.put("space_guid", "space_guid");

        Context cfContext = new Context(CLOUD_FOUNDRY_PLATFORM, cfContextProps);

        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(
                "coab-serviceid",
                "coab-planid",
                bindResource,
                cfContext,
                serviceBindingParams
        );
        request.withBindingId("service-instance-binding-id");
        request.withServiceInstanceId(serviceInstanceId);
        request.withApiInfoLocation("api-info");
        request.withOriginatingIdentity(aContext());
        request.withCfInstanceId("cf-instance-id");
        return request;
    }

    static CreateServiceInstanceAppBindingResponse aBindingResponse() {
        CreateServiceInstanceAppBindingResponse expectedResponse = new CreateServiceInstanceAppBindingResponse();
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("contact-points", "127.0.0.1");
        credentials.put("password", "aPassword");
        credentials.put("port", "9142");
        credentials.put("jdbcUrl", "jdbc:cassandra://127.0.0.1:9142/ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("login", "rbbbbbbbb_ba66_4841_018d_2e4d4ce91f47");
        expectedResponse.withCredentials(credentials);
        expectedResponse.withBindingExisted(false);
        return expectedResponse;
    }

    public static Context aContext() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ORIGINATING_USER_KEY, "user_guid");
        properties.put(ORIGINATING_EMAIL_KEY, "user_email");
        return new Context(ORIGINATING_CLOUDFOUNDRY_PLATFORM, properties);
    }

    @SuppressWarnings("WeakerAccess")
    public static CreateServiceInstanceRequest aCreateServiceInstanceRequest(){
        //Given a parameter request
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameterName", "parameterValue");

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );
        request.withServiceInstanceId("service-instance-guid");
        return request;
    }

    static DeleteServiceInstanceRequest aDeleteServiceInstanceRequest() {
        // Given an incoming delete request
        return new DeleteServiceInstanceRequest("instance_id",
                "service_id",
                "plan_id",
                new ServiceDefinition(),
                true);
    }

    public static UpdateServiceInstanceRequest anUpdateServiceInstanceRequest() {
        // Given an incoming delete request
        return new UpdateServiceInstanceRequest(
                "service_id",
                "plan_id",
                new HashMap<>())
                .withServiceInstanceId("instance_id");
    }
}
