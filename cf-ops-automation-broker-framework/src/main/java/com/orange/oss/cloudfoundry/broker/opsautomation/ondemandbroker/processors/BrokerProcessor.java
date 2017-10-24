package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

public interface BrokerProcessor {

	public void preCreate(Context ctx);
	public void postCreate(Context ctx);
	public void preBind(Context ctx);
	public void postBind(Context ctx);
	
	
	public void preDelete(Context ctx);
	public void postDelete(Context ctx);
	
	public void preUnBind(Context ctx);
	public void postUnBind(Context ctx);

	
}
