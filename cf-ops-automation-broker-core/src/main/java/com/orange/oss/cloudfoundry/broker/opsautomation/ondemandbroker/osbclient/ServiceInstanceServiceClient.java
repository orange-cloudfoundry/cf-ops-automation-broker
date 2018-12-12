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

import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static org.springframework.cloud.servicebroker.model.ServiceBrokerRequest.API_INFO_LOCATION_HEADER;
import static org.springframework.cloud.servicebroker.model.ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER;
import static org.springframework.cloud.servicebroker.model.instance.AsyncServiceInstanceRequest.ASYNC_REQUEST_PARAMETER;

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
//            "/{cfInstanceId}/v2/service_instances/{instanceId}",
            "/v2/service_instances/{instanceId}"
    }, method = RequestMethod.PUT)
    ResponseEntity<CreateServiceInstanceResponse> createServiceInstance(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable("instanceId") String serviceInstanceId,
            @RequestParam(value = ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @Valid @RequestBody CreateServiceInstanceRequest request);

    @RequestMapping(value = {
//            "/{cfInstanceId}/v2/service_instances/{instanceId}/last_operation",
            "/v2/service_instances/{instanceId}/last_operation"
    }, method = RequestMethod.GET)
    public ResponseEntity<GetLastServiceOperationResponse> getServiceInstanceLastOperation(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable("instanceId") String serviceInstanceId,
            @RequestParam("service_id") String serviceDefinitionId,
            @RequestParam("plan_id") String planId,
            @RequestParam(value = "operation", required = false) String operation,
            @RequestHeader(value = API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString);


    @RequestMapping(value = {
//            "/{cfInstanceId}/v2/service_instances/{instanceId}",
            "/v2/service_instances/{instanceId}"
    }, method = RequestMethod.DELETE)
    public ResponseEntity<DeleteServiceInstanceResponse> deleteServiceInstance(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable("instanceId") String serviceInstanceId,
            @RequestParam("service_id") String serviceDefinitionId,
            @RequestParam("plan_id") String planId,
            @RequestParam(value = ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString);


    @RequestMapping(value = {
//            "/{cfInstanceId}/v2/service_instances/{instanceId}",
            "/v2/service_instances/{instanceId}"
    }, method = RequestMethod.PATCH)
    public ResponseEntity<UpdateServiceInstanceResponse> updateServiceInstance(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable("instanceId") String serviceInstanceId,
            @RequestParam(value = ASYNC_REQUEST_PARAMETER, required = false) boolean acceptsIncomplete,
            @RequestHeader(value = API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @Valid @RequestBody UpdateServiceInstanceRequest request);

//    @RequestMapping(value = "/v2/service_instances/{instanceId}", method = RequestMethod.PATCH, produces = {MediaType.APPLICATION_JSON_VALUE})
//    ResponseEntity<UpdateServiceInstanceResponse> updateServiceInstance(@PathVariable("instanceId") String serviceInstanceId,
//                                                                        @Valid @RequestBody UpdateServiceInstanceRequest request,
//                                                                        @RequestParam(value = "accepts_incomplete", required = false) boolean acceptsIncomplete);
}
