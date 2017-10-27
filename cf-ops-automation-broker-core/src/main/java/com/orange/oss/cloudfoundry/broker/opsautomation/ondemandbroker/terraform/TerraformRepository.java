package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import java.util.List;

/**
 *
 */
public interface TerraformRepository {

    TerraformModule getByModuleName(String moduleName);
}
