package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.UserFacingRuntimeException;
import org.junit.Test;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;

public class ProcessorChainServiceHelperTest {

    @Test
    public void filters_internal_exceptions_details() {
        //given an exception with confidential internal data thrown
        IOException rootCause = new IOException();
        RuntimeException confidentialException = new RuntimeException("unable to push at https://login:pwd@mygit.site.org/secret_path", rootCause);

        //when
        RuntimeException wrappedException = ProcessorChainServiceHelper.processInternalException(confidentialException);

        //then the exception is wrapped into a runtime exception, hidding the confidential data

        assertThat(wrappedException.toString()).doesNotContain("login");
        assertThat(wrappedException.toString()).containsIgnoringCase("internal");
        assertThat(wrappedException.toString()).containsIgnoringCase("contact");
    }

    @Test
    public void does_not_filter_user_facing_exception() {

        //given an exception with confidential internal data thrown
        RuntimeException safeException = new UserFacingRuntimeException("invalid parameter param with value. Param should only contain alphanumerics");

        //when
        RuntimeException exception = ProcessorChainServiceHelper.processInternalException(safeException);

        //then the exception is wrapped into a runtime exception, hidding the confidential data

        assertThat(exception.toString()).contains("alphanumerics");
    }


}