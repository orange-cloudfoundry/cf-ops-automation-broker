package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.nio.file.Path;

public interface SecretsReader {

    Path getTargetManifestFilePath(Path workDir, String serviceInstanceId);

    boolean isBoshDeploymentAvailable(Path secretsWorkDir, String serviceInstanceId);

    /**
     * Parses completion marker.
     * Prereq: isBoshDeploymentAvailable() returns true for same arguments
     */
    CoabVarsFileDto getBoshDeploymentCompletionMarker(Path secretsWorkDir, String serviceInstanceId) throws IOException;
}
