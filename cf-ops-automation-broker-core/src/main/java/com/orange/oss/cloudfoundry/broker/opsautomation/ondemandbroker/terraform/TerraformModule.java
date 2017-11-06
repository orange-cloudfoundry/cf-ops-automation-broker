package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Terraform module invocation.
 */
public class TerraformModule {

	public String moduleName;
	public String source;
	public Map<String, String> properties = new HashMap<String, String>();

	public void addProperty(String key, String value) {
			properties.put(key, value);
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TerraformModule that = (TerraformModule) o;

		if (!moduleName.equals(that.moduleName)) return false;
		if (!source.equals(that.source)) return false;
		return properties.equals(that.properties);
	}

	@Override
	public int hashCode() {
		int result = moduleName.hashCode();
		result = 31 * result + source.hashCode();
		result = 31 * result + properties.hashCode();
		return result;
	}
}
