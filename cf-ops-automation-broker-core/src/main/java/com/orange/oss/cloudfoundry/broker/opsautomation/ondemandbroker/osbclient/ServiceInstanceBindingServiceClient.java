/*
 * <!--
 *
 *     Copyright (C) 2015 Orange
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 * -->
 */

package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import org.springframework.cloud.servicebroker.model.AsyncServiceBrokerRequest;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * SpringMVC annotations for the OSB client.
 * Extracted from ServiceInstanceBindingController
 *
 * ServiceInstanceBindingController does not expose interfaces, and its annotation have two variants,
 * which is not supported by spring-cloud-netflix (feign support) and triggers the following exception
 * <pre>
 * java.lang.IllegalStateException: Method createServiceInstanceBinding can only contain at most 1 value field. Found:
 * [/{cfInstanceId}/v2/service_instances/{instanceId}/service_bindings/{bindingId}, /v2/service_instances/{instanceId}/service_bindings/{bindingId}]
 * </pre>
 * As a result, we duplicate ServiceInstanceBindingController annotations, commenting out the support for multiple CF instances to keep a single annotation
 * @see org.springframework.cloud.servicebroker.controller.ServiceInstanceBindingController
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface ServiceInstanceBindingServiceClient {

    @RequestMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}/service_bindings/{bindingId}",
            "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"
    }, method = RequestMethod.PUT)
    public ResponseEntity<CreateServiceInstanceAppBindingResponse> createServiceInstanceBinding(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @PathVariable(ServiceBrokerRequest.BINDING_ID_PATH_VARIABLE) String bindingId,
            @RequestParam(value = AsyncServiceBrokerRequest.ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @Valid @RequestBody CreateServiceInstanceBindingRequest request);


    @GetMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}/service_bindings/{bindingId}",
            "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"
    })
    public ResponseEntity<GetServiceInstanceBindingResponse> getServiceInstanceBinding(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @PathVariable(ServiceBrokerRequest.BINDING_ID_PATH_VARIABLE) String bindingId,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString);


    @GetMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}/service_bindings/{bindingId}/last_operation",
            "/v2/service_instances/{instanceId}/service_bindings/{bindingId}/last_operation"
    })
    public ResponseEntity<GetLastServiceBindingOperationResponse> getServiceInstanceBindingLastOperation(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @PathVariable(ServiceBrokerRequest.BINDING_ID_PATH_VARIABLE) String bindingId,
            @RequestParam(value = ServiceBrokerRequest.SERVICE_ID_PARAMETER, required = false) String serviceDefinitionId,
            @RequestParam(value = ServiceBrokerRequest.PLAN_ID_PARAMETER, required = false) String planId,
            @RequestParam(value = "operation", required = false) String operation,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString);

    @RequestMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}/service_bindings/{bindingId}",
            "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"
    }, method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteServiceInstanceBinding(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @PathVariable(ServiceBrokerRequest.BINDING_ID_PATH_VARIABLE) String bindingId,
            @RequestParam(ServiceBrokerRequest.SERVICE_ID_PARAMETER) String serviceDefinitionId,
            @RequestParam(ServiceBrokerRequest.PLAN_ID_PARAMETER) String planId,
            @RequestParam(value = AsyncServiceBrokerRequest.ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString);
}