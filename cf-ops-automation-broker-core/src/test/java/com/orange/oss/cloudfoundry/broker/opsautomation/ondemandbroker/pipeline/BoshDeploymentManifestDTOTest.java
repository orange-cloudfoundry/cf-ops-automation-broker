package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jetbrains.annotations.NotNull;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoshDeploymentManifestDTOTest {

	VarsFilesYmlFormatter formatter = new VarsFilesYmlFormatter(false);

	@Test
	void parsesBoshManifestWithCompletionTracker() throws IOException, URISyntaxException {
		//Given a bosh manifest is ready (in src/test/resources)

		//When parsing it
		Path pathToBoshManifestFile = getResourcePath("/coab-bosh-manifests/manifest-with-completion-marker.yml");
		CoabVarsFileDto coabVarsFileDto = formatter.parseFromBoshManifestYml(pathToBoshManifestFile);

		//Then it extracts the expected completion marker
		CoabVarsFileDto expectedCoabVarsFileDto = new CoabVarsFileDto();
		expectedCoabVarsFileDto.deployment_name = "noop";
		assertThat(coabVarsFileDto).isEqualTo(expectedCoabVarsFileDto);
	}

	@Test
	void parsesBoshManifestWithoutCompletionTracker() throws IOException, URISyntaxException {
		//Given a bosh manifest is ready (in src/test/resources)

		//When parsing it
		VarsFilesYmlFormatter formatter = new VarsFilesYmlFormatter(false);
		Path pathToBoshManifestFile = getResourcePath("/coab-bosh-manifests/manifest-without-completion-marker.yml");
		CoabVarsFileDto coabVarsFileDto = formatter.parseFromBoshManifestYml(pathToBoshManifestFile);

		//Then it extracts null
		assertThat(coabVarsFileDto).isNull();
	}

	@NotNull
	private Path getResourcePath(String classPathResource) throws URISyntaxException {
		return Paths.get(this.getClass().getResource(
			classPathResource).toURI());
	}


}