package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.MaintenanceInfo;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants.ORIGINATING_EMAIL_KEY;
import static com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants.ORIGINATING_USER_KEY;
import static java.util.Arrays.asList;

public class OsbBuilderHelper {


    public static final String SERVICE_DEFINITION_ID = "service_definition_id";
    public static final String SERVICE_PLAN_ID = "plan_id";
    public static final String SMALL_PLAN_ID = SERVICE_PLAN_ID;
    public static final String SERVICE_PLAN_NAME = "small_plan_name";
    public static final String SMALL_PLAN_NAME = SERVICE_PLAN_NAME;
    public static final String UPGRADED_SERVICE_PLAN_ID = "plan_id2";
    public static final String MEDIUM_SERVICE_PLAN_ID = UPGRADED_SERVICE_PLAN_ID;
    public static final String UPGRADED_SERVICE_PLAN_NAME = "medium_plan_name";
    public static final String MEDIUM_PLAN_NAME = UPGRADED_SERVICE_PLAN_NAME;

    public static final String NEXT_UPGRADEABLE_PLAN_ID = "plan_id3";

    public static final String NEXT_UPGRADEABLE_PLAN_NAME = "large_plan_name";

    public static final String LARGE_PLAN_NAME = NEXT_UPGRADEABLE_PLAN_NAME;

    @SuppressWarnings("WeakerAccess")
    public static DeleteServiceInstanceBindingRequest anUnbindRequest(String serviceInstanceId, String bindingId) {
        ServiceDefinition serviceDefinition = aCatalog().getServiceDefinitions().get(0);
        return DeleteServiceInstanceBindingRequest.builder()
            .serviceDefinition(serviceDefinition)
                .bindingId(bindingId)
                .serviceDefinitionId(SERVICE_DEFINITION_ID)
                .planId(SERVICE_PLAN_ID)
                .serviceDefinition(serviceDefinition)
                .apiInfoLocation("api-info")
                .originatingIdentity(aCfUserContext())
                .platformInstanceId("cf-instance-id").build();
    }

    public static Catalog aCatalog() {
        Plan plan  = aSmallPlan();
        Plan plan2 = aMediumPlan();
        Plan plan3 = aLargePlan();
        ServiceDefinition serviceDefinition =  ServiceDefinition.builder()
                .id("service_id")
                .name("service_name")
                .description("service_description")
                .bindable(true)
                .plans(asList(plan, plan2, plan3)).build();

        ServiceDefinition serviceDefinition2 = ServiceDefinition.builder()
                .id("service_id2")
                .name("service_name2")
                .description("service_description3")
                .bindable(true)
                .plans(Collections.singletonList(plan3)).build();
        return Catalog.builder().serviceDefinitions(serviceDefinition
//                , serviceDefinition2 //not yet used
        ).build();
    }

    public static Plan aLargePlan() {
        return Plan.builder().id(NEXT_UPGRADEABLE_PLAN_ID).name(NEXT_UPGRADEABLE_PLAN_NAME).description("plan_description3").metadata(new HashMap<>()).build();
    }

    public static Plan aSmallPlan() {
        return Plan
            .builder().id(SERVICE_PLAN_ID).name(SERVICE_PLAN_NAME).description("plan_description").metadata(new HashMap<>()).build();
    }

    public static Plan aMediumPlan() {
        return Plan.builder().id(UPGRADED_SERVICE_PLAN_ID).name(UPGRADED_SERVICE_PLAN_NAME).description("plan_description2").metadata(new HashMap<>()).build();
    }

    public static CreateServiceInstanceBindingRequest aBindingRequest(String serviceInstanceId) {
        Map<String, Object> routeBindingParams= new HashMap<>();
        Map<String, Object> serviceBindingParams= new HashMap<>();
        serviceBindingParams.put("user-name", "myname");
        BindResource bindResource = BindResource.builder()
                .appGuid("app_guid")
                .route(null)
                .properties(routeBindingParams)
                .build();

        Context cfContext = aCfContext();

        return CreateServiceInstanceBindingRequest.builder()
                .serviceDefinitionId(SERVICE_DEFINITION_ID)
                .planId(SERVICE_PLAN_ID)
                .bindResource(bindResource)
                .context(cfContext)
                .parameters(serviceBindingParams)
                .bindingId("service-instance-binding-id")
                .serviceInstanceId(serviceInstanceId)
                .apiInfoLocation("api-info")
                .originatingIdentity(aCfUserContext())
                .platformInstanceId("cf-instance-id")
                .build();
    }

    public static Context aCfContext() {
        return CloudFoundryContext.builder()
                .organizationGuid("org_guid")
                .spaceGuid("space_guid")
                .build();
    }

    static CreateServiceInstanceAppBindingResponse aBindingResponse() {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("keyspaceName", "ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("contact-points", "127.0.0.1");
        credentials.put("password", "aPassword");
        credentials.put("port", "9142");
        credentials.put("jdbcUrl", "jdbc:cassandra://127.0.0.1:9142/ks055d0899_018d_4841_ba66_2e4d4ce91f47");
        credentials.put("login", "rbbbbbbbb_ba66_4841_018d_2e4d4ce91f47");
        return CreateServiceInstanceAppBindingResponse.builder()
                .credentials(credentials)
                .bindingExisted(false)
                .build();
    }

    public static Context aCfUserContext() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ORIGINATING_USER_KEY, "user_guid");
        properties.put(ORIGINATING_EMAIL_KEY, "user_email");

        return CloudFoundryContext.builder()
            .organizationGuid("org_id")
            .spaceGuid("space_id")
            .properties(properties)
            .build();
    }

    @SuppressWarnings("WeakerAccess")
    public static CreateServiceInstanceRequest aCreateServiceInstanceRequest(){
        //Given a parameter request
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameterName", "parameterValue");

        return CreateServiceInstanceRequest.builder()
                .serviceDefinitionId(SERVICE_DEFINITION_ID)
                .planId(MEDIUM_SERVICE_PLAN_ID)
                .parameters(parameters)
                .serviceInstanceId("service-instance-guid")
                .maintenanceInfo(anInitialMaintenanceInfo())
                .context(aCfUserContext())
                .build();
    }

    public static DeleteServiceInstanceRequest aDeleteServiceInstanceRequest() {
        // Given an incoming delete request
        return DeleteServiceInstanceRequest.builder()
                .serviceInstanceId("instance_id")
                .serviceDefinitionId("service_id")
                .planId(SERVICE_PLAN_ID)
                .serviceDefinition(ServiceDefinition.builder().build())
                .asyncAccepted(true)
                .build();
    }

    /**
     * Constructs a "cf update-service -c "{}" request
     */
    public static UpdateServiceInstanceRequest anUpdateServiceInstanceRequest() {
        // Given an incoming delete request
        return UpdateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_id")
                .serviceDefinition(aCatalog().getServiceDefinitions().get(0))
                .planId(SERVICE_PLAN_ID)
                .plan(aSmallPlan())
                .parameters(new HashMap<>())
                .serviceInstanceId("instance_id")
                .maintenanceInfo( anInitialMaintenanceInfo())
                .previousValues(new UpdateServiceInstanceRequest.PreviousValues(
                    SERVICE_PLAN_ID,
                    anInitialMaintenanceInfo()))

                .build();
    }
    /**
     * Constructs a "cf update-service -c "{}" request
     */
    public static UpdateServiceInstanceRequest anUpdateServiceInstanceRequestWithoutPreviousValue() {
        // Given an incoming delete request
        return UpdateServiceInstanceRequest.builder()
                .serviceDefinitionId("service_id")
                .serviceDefinition(aCatalog().getServiceDefinitions().get(0))
                .planId(SERVICE_PLAN_ID)
                .plan(aSmallPlan())
                .parameters(new HashMap<>())
                .serviceInstanceId("instance_id")
                .maintenanceInfo( anInitialMaintenanceInfo())

                .build();
    }

    /**
     * Constructs a "cf update-service -p plan2" request from plan 1
     */
    public static UpdateServiceInstanceRequest aPlanUpdateServiceInstanceRequest() {
        return UpdateServiceInstanceRequest.builder()
            .serviceDefinitionId("service_id")
            .serviceDefinition(aCatalog().getServiceDefinitions().get(0))
            .planId(LARGE_PLAN_NAME)
            .plan(aLargePlan())
            .serviceInstanceId("instance_id")
            .maintenanceInfo( anInitialMaintenanceInfo())
            .parameters(new HashMap<>())
            .previousValues(new UpdateServiceInstanceRequest.PreviousValues(
                MEDIUM_SERVICE_PLAN_ID,
                null))
            .context(aCfUserContext())
            .originatingIdentity(aCfUserContext())
            .build();
    }
    /**
     * Constructs a "cf update-service -p plan1" request from plan 2
     */
    public static UpdateServiceInstanceRequest aPlanDowngradeServiceInstanceRequest() {
        return UpdateServiceInstanceRequest.builder()
            .serviceDefinitionId("service_id")
            .serviceDefinition(aCatalog().getServiceDefinitions().get(0))
            .planId(SMALL_PLAN_ID)
            .plan(aSmallPlan())
            .serviceInstanceId("instance_id")
            .maintenanceInfo( anInitialMaintenanceInfo())
            .parameters(new HashMap<>())
            .previousValues(new UpdateServiceInstanceRequest.PreviousValues(
                MEDIUM_SERVICE_PLAN_ID,
                null))
            .context(aCfUserContext())
            .originatingIdentity(aCfUserContext())
            .build();
    }

    /**
     * Constructs a "cf update-service --upgrade" request
     */
    public static UpdateServiceInstanceRequest anUpgradeServiceInstanceRequest() {
        return UpdateServiceInstanceRequest.builder()
            .serviceDefinitionId("service_id")
            .serviceDefinition(aCatalog().getServiceDefinitions().get(0))
            .planId(SERVICE_PLAN_ID)
            .plan(aCatalog().getServiceDefinitions().get(0).getPlans().get(0))
            .parameters(new HashMap<>())
            .serviceInstanceId("instance_id")
            .maintenanceInfo(anUpgradedMaintenanceInfo())
            .previousValues(new UpdateServiceInstanceRequest.PreviousValues(
                SERVICE_PLAN_ID,
                anInitialMaintenanceInfo()))
            .build();
    }




    public static MaintenanceInfo anInitialMaintenanceInfo() {
        return MaintenanceInfo.builder()
            .version(1, 0, 0, "")
            .description("Initial version")
            .build();
    }

    public static MaintenanceInfo anUpgradedMaintenanceInfo() {
        return MaintenanceInfo.builder()
            .version(2, 0, 0, "")
            .description("Includes dashboards")
            .build();
    }

    public static GetServiceInstanceRequest aGetServiceInstanceRequest() {
        return GetServiceInstanceRequest.builder()
                .serviceInstanceId("instance_id")
                .build();
    }

    @NotNull
    public static Map<String, Object> osbCmdbCustomParam(String brokeredServiceGuid) {
        Map<String, Map<String,String>> osbCmdbMetaData = new HashMap<>();
        osbCmdbMetaData.put(BoshProcessor.CMDB_LABELS_KEY,
            Collections.singletonMap(BoshProcessor.CMDB_BROKERED_SERVICE_INSTANCE_GUID_KEY,
                brokeredServiceGuid));
        return Collections
            .singletonMap(BoshProcessor.X_OSB_CMDB_CUSTOM_KEY_NAME, osbCmdbMetaData);
    }

}
