package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import java.util.List;

/**
 *
 */
public interface TerraformRepository {

    @SuppressWarnings("UnusedReturnValue")
    TerraformModule save(TerraformModule module);

    TerraformModule getByModuleName(String moduleName);

    /**
     * @return null if there is no matching module
     */
    TerraformModule getByModuleProperty(String propertyName, String propertyValue);

    void delete(TerraformModule module);
}
