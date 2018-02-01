package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;

/**
 * Created by ijly7474 on 15/12/17.
 */
public interface StructureGenerator {

    void checkPrerequisites(Path workDir);

    void generate(Path workDir, String serviceInstanceId);

}
