package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import okhttp3.Request;
import org.junit.Test;

public class FeignTest {

    @Test(expected = IllegalArgumentException.class)
    public void feign_rejects_invalid_too_long_urls() {
        Request.Builder builder = new Request.Builder();
        Request.Builder url = builder.url("https://cassandra-brokercassandravarsops_1cc4bd10-aadc-4d7d-a1c4-acb955e637db.redacted-domain.org/v2/catalog");
    }
}
