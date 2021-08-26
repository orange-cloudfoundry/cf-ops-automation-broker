package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.web.server.ResponseStatusException;

class ProcessorChainServiceHelper {

    static RuntimeException processInternalException(RuntimeException exception) {
        if (exception instanceof UserFacingRuntimeException) {
            return exception;
        } else if (exception instanceof ResponseStatusException) {
            //flow up spring response status exception mapped by OsbProxy directly as to preserve status code.
            //Did not find programmatic status code support in sc-osb service broker exception
            return exception;
        } else if (isDefinedInServiceBrokerExceptionPackage(exception)) {
            //flow up sc-osb exceptions directly
            return exception;
        } else {
            return new RuntimeException("Internal error condition. Please contact admin to have him check broker logs"); //filter out potential confidential data
        }
    }

    private static boolean isDefinedInServiceBrokerExceptionPackage(RuntimeException exception) {
        return exception.getClass().getPackage().equals(ServiceInstanceDoesNotExistException.class.getPackage());
    }

}
