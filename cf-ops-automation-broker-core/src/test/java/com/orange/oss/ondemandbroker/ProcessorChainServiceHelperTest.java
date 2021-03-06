package com.orange.oss.ondemandbroker;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.UserFacingRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

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

        //then the exception is not wrapped
        assertThat(exception).isSameAs(safeException);
    }

    @Test
    public void does_not_filter_OSB_framework_exceptions() {

        //given an exception with confidential internal data thrown
        ServiceInstanceDoesNotExistException osbException = new ServiceInstanceDoesNotExistException("9ef34c99-a156-4640-b426-5788f3e1db88");
        ServiceInstanceUpdateNotSupportedException osbException2 = new ServiceInstanceUpdateNotSupportedException("msg");
        //when

        //then the exception is not wrapped
        assertThat(ProcessorChainServiceHelper.processInternalException(osbException)).isSameAs(osbException);
        assertThat(ProcessorChainServiceHelper.processInternalException(osbException2)).isSameAs(osbException2);
    }


}