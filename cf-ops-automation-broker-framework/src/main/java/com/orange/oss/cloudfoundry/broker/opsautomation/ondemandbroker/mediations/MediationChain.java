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
		Context ctx=new Context();
		for (BrokerMediation m:mediations) {
			m.preCreate(ctx);
		}
		sink.create(ctx);
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postCreate(ctx);			
		}

	}

	public void bind() {
		Context ctx=new Context();		
		for (BrokerMediation m:mediations) {
			m.preBind(ctx);
		}
		sink.bind(ctx);
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postBind(ctx);			
		}
		
	}
	
	public void unBind() {
		Context ctx=new Context();		
		for (BrokerMediation m:mediations) {
			m.preUnBind(ctx);
		}
		sink.unBind(ctx);
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postUnBind(ctx);			
		}
		
	}
	
	public void delete() {
		Context ctx=new Context();		
		for (BrokerMediation m:mediations) {
			m.preDelete(ctx);
		}
		sink.delete(ctx);
		
		for (int i=mediations.size()-1;i>=0;i--) {
			BrokerMediation m=mediations.get(i);
			m.postDelete(ctx);			
		}
		
	}
	
	
	
	
}
