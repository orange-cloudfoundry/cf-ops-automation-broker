package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class CoabVarsFileDtoTest {

	@Test
	public void simpleEqualsContract() {
		//See https://jqno.nl/equalsverifier/manual/
		EqualsVerifier.simple().forClass(CoabVarsFileDto.class).verify();
	}

}