package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git;

import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class RetryProperties {

    private int maxAttempts=4;
    private boolean exponentialBackOff=true;
    /**
     * the delay to occur between retries
     * See {@link RetryPolicy#withDelay(Duration)}
     */
    private int delayMilliSeconds=5000;
    /**
     * when backoff is turned on, the max delay for each retries
     * See {@link RetryPolicy#withBackoff(long, long, ChronoUnit)}
     */
    private int maxDelayMilliSeconds=60000;
    /**
     * the max duration to perform retries for, else the execution will be failed.
     * See {@link RetryPolicy#withMaxDuration(Duration)}
     */
    private int maxDurationMilliSeconds=50000;

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public boolean isExponentialBackOff() { return exponentialBackOff; }
    public void setExponentialBackOff(boolean exponentialBackOff) { this.exponentialBackOff = exponentialBackOff; }
    public int getDelayMilliSeconds() { return delayMilliSeconds; }
    public void setDelayMilliSeconds(int delayMilliSeconds) { this.delayMilliSeconds = delayMilliSeconds; }
    public int getMaxDelayMilliSeconds() { return maxDelayMilliSeconds; }
    public void setMaxDelayMilliSeconds(int maxDelayMilliSeconds) { this.maxDelayMilliSeconds = maxDelayMilliSeconds; }
    public int getMaxDurationMilliSeconds() { return maxDurationMilliSeconds; }
    public void setMaxDurationMilliSeconds(int maxDurationMilliSeconds) { this.maxDurationMilliSeconds = maxDurationMilliSeconds; }

    public RetryPolicy<Object> toRetryPolicy() {
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                .withMaxAttempts(maxAttempts);
        if (exponentialBackOff) {
            retryPolicy.withBackoff(delayMilliSeconds, maxDelayMilliSeconds, ChronoUnit.MILLIS);
        } else {
            if (delayMilliSeconds !=0) {
                retryPolicy.withDelay(Duration.ofMillis(delayMilliSeconds));
            }
        }
        if (maxDurationMilliSeconds !=0) {
            retryPolicy.withMaxDuration(Duration.ofMillis(maxDurationMilliSeconds));
        }
        return retryPolicy;
    }
}
