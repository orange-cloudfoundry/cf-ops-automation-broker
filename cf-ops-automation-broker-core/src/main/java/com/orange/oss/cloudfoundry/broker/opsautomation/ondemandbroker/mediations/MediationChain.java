package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class MediationChain {

	private static Logger logger=LoggerFactory.getLogger(MediationChain.class.getName());
	
	private List<BrokerMediation> mediations;
	private BrokerMediationSink sink;
	
	public MediationChain(List<BrokerMediation> mediations,BrokerMediationSink sink) {
		this.mediations=mediations;
		this.sink=sink;
	}

	public void create() {
		for (BrokerMediation m:mediations) {
			m.preCreate();
		}
		sink.create();
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postCreate();			
		}

	}

	public void bind() {
		for (BrokerMediation m:mediations) {
			m.preBind();
		}
		sink.bind();
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postBind();			
		}
		
	}
	
	public void unBind() {
		for (BrokerMediation m:mediations) {
			m.preUnBind();
		}
		sink.unBind();
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postUnBind();			
		}
		
	}
	
	public void delete() {
		for (BrokerMediation m:mediations) {
			m.preDelete();
		}
		sink.delete();
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postDelete();			
		}
		
	}
	
	
	
	
}
