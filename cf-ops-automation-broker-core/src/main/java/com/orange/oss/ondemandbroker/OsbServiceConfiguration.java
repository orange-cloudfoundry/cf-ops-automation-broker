package com.orange.oss.ondemandbroker;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OsbServiceConfiguration {

    /**
     * Fail fast on missing catalog configuration.
     * Otherwise, spring cloud open service broker would silently not
     * load controllers, and tomcat will still start successfully, and return 404 for any OSB request.
     */
//    @ConditionalOnMissingBean(value = Catalog.class)
//    @Bean
    public Catalog failFastOnMissingCatalogWithConditional() {
        throw new RuntimeException("Missing a catalog bean declaration. Check mandatory \"spring.cloud.openservicebroker.catalog\" config property (renamed from previously CATALOG_YML)");
    }

}
