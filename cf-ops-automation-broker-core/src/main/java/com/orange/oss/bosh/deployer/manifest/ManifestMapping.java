package com.orange.oss.bosh.deployer.manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ManifestMapping {

	public static class Manifest {

		
		public String director_uuid;
		public String name;
		public List<Release> releases=new ArrayList<Release>();
		public Update update;
		public List<Stemcell> stemcells=new ArrayList<Stemcell>();
		public List<InstanceGroup> instance_groups=new ArrayList<InstanceGroup>();
		
		//see: http://yuluer.com/page/ehidecb-how-can-i-include-raw-json-in-an-object-using-jackson.shtml
//		public Map<String,Object> properties=new HashMap<String,Object>(); // yaml structure
//		@JsonIgnore
//		Object props;
//		
//		@JsonRawValue
//		public String getProperties() {
//			return props == null ? "[]" : props.toString();
//		}
//
//		public void setProperties(JsonNode node) {
//			this.props = node;
//		}
		
		public Object properties=new HashMap<String, Object>();

		
	}

	public static class Release {
		public String name;
		public String version;
	}

	public static class Update {
		public int canaries;
		public String canary_watch_time; // 30000-240000
		public String update_watch_time; // 30000-240000
		
		public int max_in_flight; // 1 #<-- important to limit max in flight,
									// for consul update, and for hazelcast
									// cluster smooth mem migration
		public boolean serial;
	}

	public static class Stemcell {
		public String alias; // trusty
		public String os; // ubuntu-trusty
		public String version; // latest
	}

	@JsonInclude(Include.NON_NULL)
	public static class InstanceGroup {
		public String name;
		public int instances; // 1
		
		public String vm_type; // default
		public String stemcell; // trusty
		
		public String lifecycle; //errand
		public String persistent_disk_type; //name in cloudconfig. optional ?
		public List<String> azs;// [z1]
		public List<Network> networks;
		public List<Job> jobs;
		
		
		//see: http://yuluer.com/page/ehidecb-how-can-i-include-raw-json-in-an-object-using-jackson.shtml
		
//		@JsonIgnore
//		Object props;
//		
//		@JsonRawValue
//		public String getProperties() {
//			return props == null ? "[]" : props.toString();
//		}
//
//		public void setProperties(JsonNode node) {
//			this.props = node;
//		}
		
		public Object properties=new HashMap<String, Object>();

		//public Map<String,Object> properties=new HashMap<String,Object>();; // deprecated in favor of job level properties
	}

	public static class Network {
		public String name; // networks: [{name: net-hazelcast}]
	}

	public static class Job {
		public String name;
		public String release;
		
		public Map<String,Object> consumes=new HashMap<String, Object>(); //FIXME: correctly parse link
		public Map<String,Object> provides=new HashMap<String, Object>(); ////FIXME: correctly parse link
		
		
		//public Map<String,Object> properties=new HashMap<String,Object>();// job level properties
		//see: http://yuluer.com/page/ehidecb-how-can-i-include-raw-json-in-an-object-using-jackson.shtml
//		public Map<String,Object> properties=new HashMap<String,Object>(); // yaml structure
//		@JsonIgnore
//		Object props;
//		@JsonRawValue
//		public String getProperties() {
//			return props == null ? "[]" : props.toString();
//		}
//		public void setProperties(JsonNode node) {
//			this.props = node;
//		}
		public Object properties=new HashMap<String, Object>();

		
		
//		public List<Link> consumes; //FIXME: correctly parse link
//		public List<Link> provides; ////FIXME: correctly parse link
		
		
	}

	public static class Link {
		public String name;
		public Map<String, String> attributes;
	}

}
