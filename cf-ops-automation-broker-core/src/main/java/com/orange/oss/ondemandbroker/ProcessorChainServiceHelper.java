package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.UserFacingRuntimeException;

class ProcessorChainServiceHelper {

    static RuntimeException processInternalException(RuntimeException exception) {
        if (exception instanceof UserFacingRuntimeException) {
            return exception;
        } else {
            return new RuntimeException("Internal error condition. Please contact admin to have him check broker logs"); //filter out potential confidential data
        }
    }
}
