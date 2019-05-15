package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import net.jodah.failsafe.RetryPolicy;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryPropertiesTest {

    private Map<String, String> map;

    @Before
    public void setUp() {
        this.map = new HashMap<>();
    }

    @Test
    public void binds_maxAttempts() {
        map.put("git.paas-template.retry.maxAttempts", "10");
        RetryProperties properties = bindProperties();
        assertThat(properties.getMaxAttempts()).isEqualTo(10);
    }

    @Test(expected = Exception.class)
    public void rejects_negative_delay() {
        map.put("git.paas-template.retry.delayMilliSeconds", "-1");
        bindProperties().toRetryPolicy();
    }

    @Test
    public void builds_default_retry_policy() {
        RetryProperties properties = bindProperties();
        RetryPolicy<Object> retryPolicy = properties.toRetryPolicy();
        assertThat(retryPolicy.getMaxAttempts()).isEqualTo(4);
        assertThat(retryPolicy.getDelay()).isEqualTo(Duration.ofMillis(5000));
        assertThat(retryPolicy.getDelayFactor()).isEqualTo(2d);
        assertThat(retryPolicy.getMaxDuration()).isEqualTo(Duration.ofSeconds(60));
        assertThat(retryPolicy.getMaxDuration()).isEqualTo(Duration.ofSeconds(60));
    }
    @Test
    public void builds_simple_delay_retry_policy() {
        map.put("git.paas-template.retry.maxAttempts", "10");
        map.put("git.paas-template.retry.delayMilliSeconds", "5000");
        map.put("git.paas-template.retry.exponentialBackOff", "false");
        RetryProperties properties = bindProperties();
        RetryPolicy<Object> retryPolicy = properties.toRetryPolicy();
        assertThat(retryPolicy.getMaxAttempts()).isEqualTo(10);
        assertThat(retryPolicy.getDelay()).isEqualTo(Duration.ofMillis(5000));
        assertThat(retryPolicy.getMaxDuration()).isEqualTo(Duration.ofMillis(60000));
        assertThat(retryPolicy.getDelayFactor()).isEqualTo(0d);
    }

    //Inspired from SCOSB ServiceBrokerPropertiesTest
    protected RetryProperties bindProperties() {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(this.map);
        Binder binder = new Binder(source);
        GitProperties gitProperties = new GitProperties();
        binder.bind("git.paas-template", Bindable.ofInstance(gitProperties));
        return gitProperties.getRetry();
    }


}
