package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BrokerCatalogConfig {
	@Bean
	public Catalog catalog() {
		return new Catalog(Collections.singletonList(
				new ServiceDefinition(
						"ondemand-service",
						"ondemand",
						"A simple ondemand service broker implementation",
						true,
						false,
						Collections.singletonList(
								new Plan("ondemand-plan",
										"default",
										"This is a default ondemand plan.  All services are created equally.",
										getPlanMetadata())),
						Arrays.asList("ondemand", "document"),
						getServiceDefinitionMetadata(),
						null,
						null)));
	}


	private Map<String, Object> getServiceDefinitionMetadata() {
		Map<String, Object> sdMetadata = new HashMap<>();
		sdMetadata.put("displayName", "ondemand");
		sdMetadata.put("imageUrl", "http://info.mongodb.com/rs/mongodb/images/MongoDB_Logo_Full.png");
		sdMetadata.put("longDescription", "ondemand Service");
		sdMetadata.put("providerDisplayName", "Orange");
		sdMetadata.put("documentationUrl", "https://orange.com");
		sdMetadata.put("supportUrl", "https://orange.com");
		return sdMetadata;
	}
	
	private Map<String,Object> getPlanMetadata() {
		Map<String,Object> planMetadata = new HashMap<>();
		planMetadata.put("costs", getCosts());
		planMetadata.put("bullets", getBullets());
		return planMetadata;
	}

	private List<Map<String,Object>> getCosts() {
		Map<String,Object> costsMap = new HashMap<>();
		
		Map<String,Object> amount = new HashMap<>();
		amount.put("usd", 0.0);
	
		costsMap.put("amount", amount);
		costsMap.put("unit", "MONTHLY");
		
		return Collections.singletonList(costsMap);
	}
	
	private List<String> getBullets() {
		return Arrays.asList("Dedicated ondemand server", 
				"100 MB Storage (not enforced)", 
				"40 concurrent connections (not enforced)");
	}
	

}
