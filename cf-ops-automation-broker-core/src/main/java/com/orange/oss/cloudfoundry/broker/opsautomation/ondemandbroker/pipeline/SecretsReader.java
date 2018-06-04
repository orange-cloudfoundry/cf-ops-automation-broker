package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.nio.file.Path;

public interface SecretsReader {

    Path getTargetManifestFilePath(Path workDir, String serviceInstanceId);

    boolean isBoshDeploymentAvailable(Path secretsWorkDir, String serviceInstanceId);
}
