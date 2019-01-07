package com.orange.oss.ondemandbroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OsbServiceConfiguration {

    private static Logger logger = LoggerFactory.getLogger(OsbServiceConfiguration.class.getName());


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

    /**
     * Fails even when catalog configured with:
     * Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'failFastOnMissingCatalogInConfiguration' defined in class path resource [com/orange/oss/ondemandbroker/OsbServiceConfiguration.class]: Unsatisfied dependency expressed through method 'failFastOnMissingCatalogInConfiguration' parameter 0; nested exception is org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name 'failFastOnMissingCatalogInConfiguration': Requested bean is currently in creation: Is there an unresolvable circular reference?
     * 	at org.springframework.beans.factory.support.ConstructorResolver.createArgumentArray(ConstructorResolver.java:733)
     */
//    @Bean
    public Catalog failFastOnMissingCatalogInConfiguration(@Autowired Catalog catalog) {
        return catalog;
    }


}
