package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class GitProcessorContextTest {

    @Test
    public void supports_pooleable_flag() {
        assertThat(GitProcessorContext.createBranchIfMissing.isPoolable()).isTrue();
    }

}