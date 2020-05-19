package com.orange.oss.bosh.deployer.manifest;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ManifestParser.class})
public class ManifestParserTest {

	private static Logger logger=LoggerFactory.getLogger(ManifestParserTest.class.getName());
	
	
	@Value("classpath:/manifests/hazelcast.yml")
    private Resource manifestResource;

	@Value("classpath:/manifests/all-constructs-manifest.yml")
    private Resource allConstructManifestResource;

	
	@Test
	public void testParser() throws IOException {
		ManifestParser parser=new ManifestParser();
		ManifestMapping.Manifest m=parser.parser(new String(Files.readAllBytes(manifestResource
				.getFile()
				.toPath())));
		
		logger.info("parsed manifest"+m );
		Assertions.assertThat(m.director_uuid).isNotEmpty();
	}
	
	@Test
	public void generate() {
		
		ManifestParser parser=new ManifestParser();
		ManifestMapping.Manifest m=new ManifestMapping.Manifest();
		
		String manifest=parser.generate(m);
		logger.info("generated manifest \n {}",manifest );
	}
	
	@Test
	public void parseAndGenerate() throws IOException{
		ManifestParser parser=new ManifestParser();
		ManifestMapping.Manifest m=parser.parser(new String(Files.readAllBytes(allConstructManifestResource
				.getFile()
				.toPath())));
		
		String generatedManifest=parser.generate(m);
		logger.info("generated manifest \n {}",generatedManifest );		
		//Assert Equivalent yaml ?
	}

	@Test
	public void testParseAllConstucts() throws IOException {
		ManifestParser parser=new ManifestParser();
		ManifestMapping.Manifest m=parser.parser(new String(Files.readAllBytes(this.allConstructManifestResource
				.getFile()
				.toPath())));
		
		logger.info("parsed manifest"+m );
		assertThat(m.director_uuid).isNotEmpty();
		assertThat(m.name).isEqualTo("name");
		
		assertThat(m.update.canaries).isNotNull();
		assertThat(m.update.canary_watch_time).isNotNull();
		assertThat(m.update.max_in_flight).isNotNull();
		assertThat(m.update.serial).isNotNull();
		
		
		assertThat(m.stemcells.get(0).os).isNotEmpty();
		assertThat(m.stemcells.get(0).alias).isNotEmpty();
		assertThat(m.stemcells.get(0).version).isNotEmpty();
		
		assertThat(m.releases.get(0).name).isNotEmpty();
		assertThat(m.releases.get(0).version).isNotEmpty();
		
		
		assertThat(m.instance_groups.get(0).name).isEqualTo("test-errand");
		assertThat(m.instance_groups.get(0).instances).isPositive();
		assertThat(m.instance_groups.get(0).persistent_disk_type).isNull();
//		assertThat(m.instance_groups.get(0).life_cycle).isNotEmpty();
		
		assertThat(m.instance_groups.get(0).azs).isNotEmpty();
		assertThat(m.instance_groups.get(0).azs.get(0)).isNotEmpty();		
		
		assertThat(m.instance_groups.get(0).networks).isNotEmpty();
		assertThat(m.instance_groups.get(0).networks.get(0).name).isNotEmpty();

		assertThat(m.instance_groups.get(0).jobs).isNotEmpty();	
		assertThat(m.instance_groups.get(0).jobs.size()).isPositive();

		assertThat(m.instance_groups.get(0).jobs.get(0).name).isNotEmpty();
		assertThat(m.instance_groups.get(0).jobs.get(0).release).isNotEmpty();
		assertThat(m.instance_groups.get(0).jobs.get(0).consumes).isNotEmpty();
		assertThat(m.instance_groups.get(0).jobs.get(0).provides).isNotEmpty();
		
		assertThat(m.instance_groups.get(0).jobs.get(0).properties).isNotNull();
		
		//FIXME: parse properties inside Rawjson
		Object nodeProps=m.instance_groups.get(0).jobs.get(0).properties;
		Map props=(Map) nodeProps;
		String prop1Value=(String) props.get("prop1");
		assertThat(prop1Value).isEqualTo("valueprop1");
		
		Map props2=(Map) props.get("prop2");
		String prop21value=(String) props2.get("prop2_1");
		assertThat(prop21value).isEqualTo("value_prop2_1");
		
	}
	
	
	

}
