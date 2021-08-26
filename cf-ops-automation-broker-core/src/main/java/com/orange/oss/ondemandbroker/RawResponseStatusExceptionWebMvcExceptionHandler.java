/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orange.oss.ondemandbroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.servicebroker.annotation.ServiceBrokerRestController;
import org.springframework.cloud.servicebroker.controller.ServiceBrokerWebMvcExceptionHandler;
import org.springframework.cloud.servicebroker.model.error.ErrorMessage;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * By passes {@link ServiceBrokerWebMvcExceptionHandler} to map {@link ResponseStatusException} to the native http
 * status code, and the reason to the osb `description` field.
 * @see <a href="https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#service-broker-errors">osb spec section on service broker errors</a>
 *
 * This is necessary to directly return nested inner broker response codes to osb caller and avoid swallowing
 * important messages/status into default 500 errors.
 *
 * We expect nested inner broker messages to be user facing and not leak confidential details
 */
@ControllerAdvice(annotations = ServiceBrokerRestController.class)
@ResponseBody
@Order(Ordered.LOWEST_PRECEDENCE - 11) // ServiceBrokerWebMvcExceptionHandler has a precedence of lowest-10
public class RawResponseStatusExceptionWebMvcExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RawResponseStatusExceptionWebMvcExceptionHandler.class);

	@ExceptionHandler(value = {ResponseStatusException.class})
	protected ResponseEntity<Object> handleException(
		RuntimeException ex, WebRequest request) {
		LOG.info("Mapping ResponseStatusException to its native status code and message: {}", ex.toString());
		ResponseStatusException responseStatusException = (ResponseStatusException) ex;
		return handleExceptionInternal(ex, new ErrorMessage(responseStatusException.getReason()),
			new HttpHeaders(), responseStatusException.getStatus(), request);
	}

}
