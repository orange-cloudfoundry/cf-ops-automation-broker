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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URL;

/**
 * Filtered broker web client connection settings
 *
 * @author Sebastien Bortolussi
 */
@Component
@ConfigurationProperties(OsbClientProperties.PREFIX)
public class OsbClientProperties {

    public static final String PREFIX = "osbclient";
    private URL url;
    private String user;
    private String password;

    public OsbClientProperties() {
    }

    public OsbClientProperties(URL url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}


