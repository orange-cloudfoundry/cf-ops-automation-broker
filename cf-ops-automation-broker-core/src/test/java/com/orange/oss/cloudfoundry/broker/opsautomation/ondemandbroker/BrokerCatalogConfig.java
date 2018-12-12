package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BrokerCatalogConfig {
	@Bean
	public Catalog catalog() {
		return Catalog.builder().serviceDefinitions(
				ServiceDefinition.builder()
						.id("ondemand-service")
						.name("ondemand")
						.description("A simple ondemand service broker implementation")
						.bindable(true)
						.planUpdateable(false)
						.plans(Plan.builder()
								.id("ondemand-plan")
								.name("default")
								.description("This is a default ondemand plan.  All services are created equally.")
								.metadata(getServiceDefinitionMetadata())
								.build())
						.build())
				.build();
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
