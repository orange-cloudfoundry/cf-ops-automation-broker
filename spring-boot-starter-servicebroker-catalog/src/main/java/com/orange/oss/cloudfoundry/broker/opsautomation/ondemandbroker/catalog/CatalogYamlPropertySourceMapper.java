package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Utility class to map a Yaml {@link Resource}
 * into a {@link PropertySource}.
 *
 * @author Sebastien Bortolussi
 */
class CatalogYamlPropertySourceMapper {

    static PropertySource<?> toPropertySource(Resource catalogResource) throws IOException {
        YamlPropertySourceLoader sourceLoader = new YamlPropertySourceLoader();
        return sourceLoader.load("catalog_from_env_var", catalogResource).get(0);
    }
}
