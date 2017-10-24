package com.orange.oss.bosh.deployer.manifest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class. ables to transform map, with property with dot syntax to Map
 * of Map Object (compatible with Jackson Yaml mapping)
 * 
 * @author poblin-orange
 *
 */
public class PropertyMapper {

	private static Logger logger = LoggerFactory.getLogger(PropertyMapper.class.getName());

	public static Object map(Map<String, String> properties) {

		Map<String, Object> result = new HashMap<String, Object>();

		//for each key
		for (String key : properties.keySet()) {
			String[] paths = key.toString().split("\\.");
			List<String> path = new ArrayList<>(Arrays.asList(paths));
			int indexLast = path.size() - 1;
			String propertyName = path.get(indexLast);
			path.remove(indexLast);// last element is prop name
			Map<String, Object> current = result;
			for (String branch : path) {
				// creates map if required
				if (current.get(branch) == null) {
					current.put(branch, new HashMap<String, Object>());
				}
				current = (Map<String, Object>) current.get(branch);
			}
			// now put the value
			current.put(propertyName, properties.get(key));
		}
		;
		return result;

	}

}
