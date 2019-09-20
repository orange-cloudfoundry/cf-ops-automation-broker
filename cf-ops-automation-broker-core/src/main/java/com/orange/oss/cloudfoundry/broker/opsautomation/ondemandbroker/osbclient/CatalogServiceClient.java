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
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Rest client for service broker catalog endpoint using
 *
 * @author Sebastien Bortolussi
 */
public interface CatalogServiceClient {

    @RequestMapping(value = "/v2/catalog", method = RequestMethod.GET)
    Catalog getCatalog(@RequestHeader(value = OsbConstants.X_Broker_API_Version, defaultValue = OsbConstants.X_Broker_API_Version_Value) String apiVersion);
}