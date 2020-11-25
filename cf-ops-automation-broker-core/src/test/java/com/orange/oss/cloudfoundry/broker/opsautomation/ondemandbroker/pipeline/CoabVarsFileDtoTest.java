package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * see {@link VarsFilesYmlFormatterTest} for json serialization/deserialization of CoabVarsFileDto
 * see {@link CoabVarsFileDtoBuilder} for building CoabVarsFileDto out of OSB request
 */
class CoabVarsFileDtoTest {

	@Test
	public void simpleEqualsContract() {
		//See https://jqno.nl/equalsverifier/manual/
		EqualsVerifier.simple().forClass(CoabVarsFileDto.class).verify();
	}

}