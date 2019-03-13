package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.InMemoryResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * An  {@link ApplicationContextInitializer} to register a {@link PropertySource} from
 * CATALOG_YML environment variable that contains service broker catalog configuration in a Yaml format.
 *
 * @author Sebastien Bortolussi
 */
public class YamlCataloglAsEnvironmentVarApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String CATALOG_YML = "CATALOG_YML";

    private static Logger LOGGER = LoggerFactory.getLogger(YamlCataloglAsEnvironmentVarApplicationContextInitializer.class.getName());

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        try {
            String yml = configurableApplicationContext.getEnvironment().getProperty(CATALOG_YML);
            if (isEmpty(yml)) {
                return;
            }
            LOGGER.info("CATALOG_YML environment variable is set with content : {}", yml);
            Resource resource = new InMemoryResource(yml);
            LOGGER.info("Using CATALOG_YML environment variable to set service broker catalog.");
            PropertySource<?> propertySource = CatalogYamlPropertySourceMapper.toPropertySource(resource);
            convertPropertySourceToScOsbKeyPrefix((Map<String, String>) propertySource.getSource());
            configurableApplicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read CATALOG_YML environment variable.",e);
        }
    }

    void convertPropertySourceToScOsbKeyPrefix(Map<String, String> source) {
        Iterator<Map.Entry<String, String>> iterator = source.entrySet().iterator();
        Map<String, String> convertedEntries = new HashMap<>(source.size());
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String convertedKey = convertToScOsbFormat(key);
            if (!key.equals(convertedKey)) {
                iterator.remove();
                convertedEntries.put(convertedKey, entry.getValue());
            }
        }
        source.putAll(convertedEntries);
    }
    
    private String convertToScOsbFormat(String key) {
        return key.replaceFirst("^servicebroker\\.catalog\\.", "spring.cloud.openservicebroker.catalog.");
    }


}
