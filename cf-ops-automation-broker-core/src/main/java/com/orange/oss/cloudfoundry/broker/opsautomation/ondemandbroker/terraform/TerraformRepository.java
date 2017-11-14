package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import java.util.List;

/**
 *
 */
public interface TerraformRepository {

    TerraformModule getByModuleId(String moduleId);

    @SuppressWarnings("UnusedReturnValue")
    TerraformModule save(TerraformModule module);

    TerraformModule getByModuleName(String moduleName);

    TerraformModule getByModuleProperty(String propertyName, String propertyValue);
}
