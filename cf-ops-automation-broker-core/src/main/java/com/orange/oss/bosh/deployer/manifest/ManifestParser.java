package com.orange.oss.bosh.deployer.manifest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.orange.oss.bosh.deployer.manifest.ManifestMapping.Manifest;


/**
 * Bosh manifest yaml mapping
 * @author poblin-orange
 *
 */
@Configuration
public class ManifestParser {

	private static Logger logger=LoggerFactory.getLogger(ManifestParser.class.getName());
	
	/**
	 * parse a yaml text manifest as Java mapping objects
	 * @param yamlManifest
	 * @return
	 */
	public ManifestMapping.Manifest parser(String yamlManifest){
		
		ObjectMapper mapper=new ObjectMapper(new YAMLFactory());
		ManifestMapping.Manifest m=null;
		try {
			m = mapper.readValue(yamlManifest,ManifestMapping.Manifest.class);
		} catch (IOException e) {
			logger.error("failure parsing yaml: {}",e);
			throw new IllegalArgumentException(e);
		}
		return m;
		
		
	}

	/**
	 * generates bosh yaml text manifest from Manifest mapping objects
	 * @param m
	 * @return
	 */
	public String generate(Manifest yamlManifest) {

		ObjectMapper mapper=new ObjectMapper(new YAMLFactory());
		
		try {
			return mapper.writeValueAsString(yamlManifest);
		} catch (JsonProcessingException e) {
			logger.error("Unable to create yaml file"); 
			throw new IllegalArgumentException(e);
		} 
	}
	
}
