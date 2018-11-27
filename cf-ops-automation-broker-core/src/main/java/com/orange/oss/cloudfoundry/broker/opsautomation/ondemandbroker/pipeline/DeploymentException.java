package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

public class DeploymentException extends RuntimeException {
        public DeploymentException(String message) {
            super(message);
        }

    public DeploymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

