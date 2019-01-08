package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.InMemoryResource;

import java.io.IOException;

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
            configurableApplicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read CATALOG_YML environment variable.",e);
        }
    }


}
