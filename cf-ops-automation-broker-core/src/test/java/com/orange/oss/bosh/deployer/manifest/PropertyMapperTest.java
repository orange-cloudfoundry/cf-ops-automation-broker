package com.orange.oss.bosh.deployer.manifest;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.orange.oss.bosh.deployer.manifest.PropertyMapper;

public class PropertyMapperTest {

	@Test
	public void testMap() {
		Map properties=new HashMap<String,String>();
		properties.put("hazelcast.jvm.memoryMo", "3000");
		properties.put("hazelcast.group.name", "hz-group");
		properties.put("hazelcast.group.password", "eentepAxHo");
		
		
		Object o=PropertyMapper.map(properties);
		Map level1=(Map) ((Map)o).get("hazelcast");
		Map<String,String> level2=(Map<String,String>) (level1.get("jvm"));
		String memory=level2.get("memoryMo");
		assertThat(memory).isEqualTo("3000");

	}

}
