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

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbConstants;
import org.springframework.cloud.servicebroker.model.AsyncServiceBrokerRequest;
import org.springframework.cloud.servicebroker.model.ServiceBrokerRequest;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * SpringMVC annotations for the OSB client.
 * Extracted from ServiceInstanceController
 *
 * ServiceInstanceController does not expose interfaces, and its annotation have two variants,
 * which is not supported by spring-cloud-netflix (feign support) and triggers the following exception
 * <pre>
 * java.lang.IllegalStateException: Method createServiceInstance can only contain at most 1 value field. Found: [/{cfInstanceId}/v2/service_instances/{instanceId}, /v2/service_instances/{instanceId}]
 * </pre>
 * As a result, we duplicate ServiceInstanceController annotations, commenting out the support for multiple CF instances
 * to keep a single annotation
 * @see org.springframework.cloud.servicebroker.controller.ServiceInstanceController
 */
@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface ServiceInstanceServiceClient {

    @RequestMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}",
            "/v2/service_instances/{instanceId}"
    }, method = RequestMethod.PUT)
    ResponseEntity<CreateServiceInstanceResponse> createServiceInstance(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @RequestParam(value = AsyncServiceBrokerRequest.ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @RequestHeader(value = OsbConstants.X_Broker_API_Version, defaultValue = OsbConstants.X_Broker_API_Version_Value) String apiVersion,
            @Valid @RequestBody CreateServiceInstanceRequest request);

    @RequestMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}",
            "/v2/service_instances/{instanceId}"
    })
    public ResponseEntity<GetServiceInstanceResponse> getServiceInstance(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @RequestHeader(value = OsbConstants.X_Broker_API_Version, defaultValue = OsbConstants.X_Broker_API_Version_Value) String apiVersion);

    @RequestMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}/last_operation",
            "/v2/service_instances/{instanceId}/last_operation"
    }, method = RequestMethod.GET)
    public ResponseEntity<GetLastServiceOperationResponse> getServiceInstanceLastOperation(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @RequestParam(value = ServiceBrokerRequest.SERVICE_ID_PARAMETER, required = false) String serviceDefinitionId,
            @RequestParam(value = ServiceBrokerRequest.PLAN_ID_PARAMETER, required = false) String planId,
            @RequestParam(value = "operation", required = false) String operation,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @RequestHeader(value = OsbConstants.X_Broker_API_Version, defaultValue = OsbConstants.X_Broker_API_Version_Value) String apiVersion);


    @RequestMapping(value = {
//            "/{platformInstanceId}/v2/service_instances/{instanceId}",
            "/v2/service_instances/{instanceId}"
    }, method = RequestMethod.DELETE)
    public ResponseEntity<DeleteServiceInstanceResponse> deleteServiceInstance(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @RequestParam(ServiceBrokerRequest.SERVICE_ID_PARAMETER) String serviceDefinitionId,
            @RequestParam(ServiceBrokerRequest.PLAN_ID_PARAMETER) String planId,
            @RequestParam(value = AsyncServiceBrokerRequest.ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @RequestHeader(value = OsbConstants.X_Broker_API_Version, defaultValue = OsbConstants.X_Broker_API_Version_Value) String apiVersion);


    @RequestMapping(value =
//            "/{platformInstanceId}/v2/service_instances/{instanceId}",
            "/v2/service_instances/{instanceId}", method = RequestMethod.PATCH, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<UpdateServiceInstanceResponse> updateServiceInstance(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable(ServiceBrokerRequest.INSTANCE_ID_PATH_VARIABLE) String serviceInstanceId,
            @RequestParam(value = AsyncServiceBrokerRequest.ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = ServiceBrokerRequest.API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @RequestHeader(value = OsbConstants.X_Broker_API_Version, defaultValue = OsbConstants.X_Broker_API_Version_Value) String apiVersion,
            @Valid @RequestBody UpdateServiceInstanceRequest request);

}
