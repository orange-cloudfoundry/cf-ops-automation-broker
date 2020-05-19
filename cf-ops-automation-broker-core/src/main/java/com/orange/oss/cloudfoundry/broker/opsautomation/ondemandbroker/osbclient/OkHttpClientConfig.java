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

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@ConditionalOnClass(OkHttpClient.class)
public class OkHttpClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OkHttpClientConfig.class);

    private static final HttpLoggingInterceptor LOGGING_INTERCEPTOR;

    static {
        LOGGING_INTERCEPTOR = new HttpLoggingInterceptor();
        LOGGING_INTERCEPTOR.setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    @Value("${director.proxyHost:}")
    private String proxyHost;
    @Value("${director.proxyPort:0}")
    private int proxyPort;

    @Bean
    public OkHttpClient squareHttpClient() {
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;
        TrustManager[] trustAllCerts = new TrustManager[]{new TrustAllCerts()};

        SSLSocketFactory sslSocketFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            sslSocketFactory = sc.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalArgumentException(e);
        }

        log.info("===> configuring OkHttp");
        OkHttpClient.Builder ohc = new OkHttpClient.Builder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .followRedirects(true)
                .followSslRedirects(true)
                .hostnameVerifier(hostnameVerifier)
                .sslSocketFactory(sslSocketFactory, getDefaultX509TrustManager())
                .addInterceptor(LOGGING_INTERCEPTOR);

        if ((this.proxyHost != null) && (this.proxyHost.length() > 0)) {
            log.info("Activating proxy on host {} port {}", this.proxyHost, this.proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost, this.proxyPort));
            ohc.proxy(proxy);
            ohc.proxySelector(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return Collections.singletonList(proxy);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress socket, IOException e) {
                    throw new IllegalArgumentException("connection to proxy failed", e);
                }
            });
        }

        return ohc.build();
    }

    /**
     * Inspired from suggested code in javadoc at
     * {@link OkHttpClient.Builder#sslSocketFactory(SSLSocketFactory, X509TrustManager)}
     */
    @NotNull
    private X509TrustManager getDefaultX509TrustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
            }
            return (X509TrustManager) trustManagers[0];
        }
        catch (NoSuchAlgorithmException | KeyStoreException | IllegalStateException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TrustAllCerts extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{}; //see http://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            // TODO Auto-generated method stub

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            // TODO Auto-generated method stub

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            // TODO Auto-generated method stub

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            // TODO Auto-generated method stub

        }

    }


}