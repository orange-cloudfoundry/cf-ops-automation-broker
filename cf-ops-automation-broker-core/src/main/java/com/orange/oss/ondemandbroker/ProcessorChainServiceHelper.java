package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.UserFacingRuntimeException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;

class ProcessorChainServiceHelper {

    static RuntimeException processInternalException(RuntimeException exception) {
        if (exception instanceof UserFacingRuntimeException) {
            return exception;
        } else if (exception.getClass().getPackage().equals(ServiceInstanceDoesNotExistException.class.getPackage())) {
            return exception;
        } else {
            return new RuntimeException("Internal error condition. Please contact admin to have him check broker logs"); //filter out potential confidential data
        }
    }
}
