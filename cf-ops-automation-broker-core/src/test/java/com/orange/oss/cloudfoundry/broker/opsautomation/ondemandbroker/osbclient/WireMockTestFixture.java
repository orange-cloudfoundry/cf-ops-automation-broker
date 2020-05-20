package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import javax.annotation.PostConstruct;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;

import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@TestComponent
public class WireMockTestFixture {

	private WireMockServer wireMockServer;

	@PostConstruct
	void startWireMock() {
		wireMockServer = new WireMockServer(
			options()
				.port(8088)
				.httpsPort(8089)
				.notifier(new Slf4jNotifier(true))
		);
		wireMockServer.start();
	}

	public void stopWiremock() {
		wireMockServer.stop();
	}

	public void resetWiremock() {
		wireMockServer.resetAll();
	}


}
