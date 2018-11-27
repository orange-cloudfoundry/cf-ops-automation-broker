package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

public class UserFacingRuntimeException extends RuntimeException {
    public UserFacingRuntimeException(String message) {
        super(message);
    }

    public UserFacingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
