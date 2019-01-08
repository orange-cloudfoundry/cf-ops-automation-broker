package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.mapper;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.PlanProperties;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog.mapper.PlanMapper;
import org.fest.assertions.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.MapAssert.entry;


public class PlanMapperTest {

    public static final String PLAN_NAME = "dev";
    public static final String PLAN_DESCRIPTION = "a description";
    public static final String PLAN_DEV_ID = "dev";
    public static final String PLAN_DEFAULT_ID = "default";


    @Test
    public void should_map_plan_name() throws Exception {

        PlanProperties planProperties = new PlanProperties();
        planProperties.setId(PLAN_DEV_ID);
        planProperties.setName(PLAN_NAME);

        org.springframework.cloud.servicebroker.model.Plan res = PlanMapper.toServiceBrokerPlan(planProperties);

        Assertions.assertThat(res.getName()).as("toServiceDefinition plan name").isEqualTo(PLAN_NAME);
    }

    @Test
    public void should_map_plan_description() throws Exception {

        PlanProperties planProperties = new PlanProperties();
        planProperties.setId(PLAN_DEV_ID);
        planProperties.setDescription(PLAN_DESCRIPTION);

        org.springframework.cloud.servicebroker.model.Plan res = PlanMapper.toServiceBrokerPlan(planProperties);

        Assertions.assertThat(res.getDescription()).as("toServiceDefinition plan description").isEqualTo(PLAN_DESCRIPTION);
    }

    @Test
    public void should_map_plan_id() throws Exception {

        PlanProperties planProperties = new PlanProperties();
        planProperties.setId(PLAN_DEV_ID);

        org.springframework.cloud.servicebroker.model.Plan res = PlanMapper.toServiceBrokerPlan(planProperties);

        Assertions.assertThat(res.getId()).as("toServiceDefinition plan id").isEqualTo(PLAN_DEV_ID.toString());
    }

    @Test
    public void should_map_plan_metadata() throws Exception {

        PlanProperties planProperties = new PlanProperties();
        planProperties.setId(PLAN_DEV_ID);

        planProperties.setMetadata("{\"bullets\":[\"20 GB of messages\",\"20 connections\"],\"costs\":[{\"amount\":{\"usd\":99.0,\"eur\":49.0},\"unit\":\"MONTHLY\"},{\"amount\":{\"usd\":0.99,\"eur\":0.49},\"unit\":\"1GB of messages over 20GB\"}],\"displayName\":\"Big Bunny\"}");

        org.springframework.cloud.servicebroker.model.Plan res = PlanMapper.toServiceBrokerPlan(planProperties);

        Assertions.assertThat(res.getMetadata()).as("toServiceDefinition plan metadata").hasSize(3).includes(
                entry("displayName", "Big Bunny"));
        //TODO assert bullets mapping
        //TODO assert costs mapping
    }

    @Test
    public void should_map_plan_free() throws Exception {

        PlanProperties planProperties = new PlanProperties();
        planProperties.setId(PLAN_DEV_ID);

        planProperties.setFree(Boolean.FALSE);

        org.springframework.cloud.servicebroker.model.Plan res = PlanMapper.toServiceBrokerPlan(planProperties);

        Assertions.assertThat(res.isFree()).as("toServiceDefinition plan free").isFalse();
    }

    @Test
    public void should_map_plans() throws Exception {

        PlanProperties plan_Properties_default = new PlanProperties();
        plan_Properties_default.setId(PLAN_DEV_ID);

        PlanProperties plan_Properties_prod = new PlanProperties();
        plan_Properties_prod.setId(PLAN_DEFAULT_ID);


        List<org.springframework.cloud.servicebroker.model.Plan> res = PlanMapper.toServiceBrokerPlans(Arrays.asList(plan_Properties_default, plan_Properties_prod));

        Assertions.assertThat(res).as("toServiceDefinition plan_dev free").hasSize(2);
    }
}