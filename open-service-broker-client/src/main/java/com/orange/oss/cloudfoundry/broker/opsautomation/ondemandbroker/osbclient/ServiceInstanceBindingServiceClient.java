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

import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static org.springframework.cloud.servicebroker.model.ServiceBrokerRequest.API_INFO_LOCATION_HEADER;
import static org.springframework.cloud.servicebroker.model.ServiceBrokerRequest.ORIGINATING_IDENTITY_HEADER;

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
//            "/{cfInstanceId}/v2/service_instances/{instanceId}/service_bindings/{bindingId}",
            "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"
    }, method = RequestMethod.PUT)
    public ResponseEntity<?> createServiceInstanceBinding(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable("instanceId") String serviceInstanceId,
            @PathVariable("bindingId") String bindingId,
            @RequestHeader(value = API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString,
            @Valid @RequestBody CreateServiceInstanceBindingRequest request);

    @RequestMapping(value = {
//            "/{cfInstanceId}/v2/service_instances/{instanceId}/service_bindings/{bindingId}",
            "/v2/service_instances/{instanceId}/service_bindings/{bindingId}"
    }, method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteServiceInstanceBinding(
//            @PathVariable Map<String, String> pathVariables,
            @PathVariable("instanceId") String serviceInstanceId,
            @PathVariable("bindingId") String bindingId,
            @RequestParam("service_id") String serviceDefinitionId,
            @RequestParam("plan_id") String planId,
            @RequestHeader(value = API_INFO_LOCATION_HEADER, required = false) String apiInfoLocation,
            @RequestHeader(value = ORIGINATING_IDENTITY_HEADER, required = false) String originatingIdentityString);

}