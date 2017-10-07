package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;

public interface BrokerMediation {

	public void preCreate();
	public void postCreate();
	public void preBind();
	public void postBind();
	
	
	public void preDelete();
	public void postDelete();
	
	public void preUnBind();
	public void postUnBind();

	
}
