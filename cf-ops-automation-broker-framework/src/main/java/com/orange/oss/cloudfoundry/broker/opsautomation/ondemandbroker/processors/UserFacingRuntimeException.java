package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

public class UserFacingRuntimeException extends RuntimeException {
    public UserFacingRuntimeException(String message) {
        super(message);
    }

    public UserFacingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
