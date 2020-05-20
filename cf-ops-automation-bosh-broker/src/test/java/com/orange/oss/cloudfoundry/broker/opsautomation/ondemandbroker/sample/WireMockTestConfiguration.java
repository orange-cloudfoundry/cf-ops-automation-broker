package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import org.springframework.context.annotation.Bean;

/**
 * Used by tests that need to use wiremock in a springboot test with junit5
 */
public class WireMockTestConfiguration {

	@Bean
	WireMockTestFixture wireMockTestFixture() {
		return new WireMockTestFixture();
	}

}
