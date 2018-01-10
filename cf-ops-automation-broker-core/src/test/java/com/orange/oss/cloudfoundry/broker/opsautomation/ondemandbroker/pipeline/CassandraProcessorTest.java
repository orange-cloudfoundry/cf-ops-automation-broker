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
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;

public class CassandraProcessorTest {

    private static final Logger logger = LoggerFactory.getLogger(CassandraProcessorTest.class);
    public static final String SERVICE_INSTANCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0";

    @Test
    public void creates_structures_and_returns_response() {
        //TODO
        //Given
        //When
        //Then
    }

    @Test
    public void responds_to_get_last_service_operation() {
        //TODO
        //Given
        //When
        //Then
    }


    private Context aContextWithCreateRequest(String key, String value) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(key, value);
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest("service_definition_id",
                "plan_id",
                "org_id",
                "space_id",
                parameters
        );
        request.withServiceInstanceId(SERVICE_INSTANCE_ID);
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);
        return context;
    }

    private Context aContextWithOperationRequest(String key, String value) {
        //TODO
        return null;
    }


}