package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

public interface BrokerProcessor {

	void preCreate(Context ctx);
	void postCreate(Context ctx);

	void preGetLastOperation(Context ctx);
	void postGetLastOperation(Context ctx);

	void preBind(Context ctx);
	void postBind(Context ctx);
	
	
	void preDelete(Context ctx);
	void postDelete(Context ctx);
	
	void preUnBind(Context ctx);
	void postUnBind(Context ctx);
	
}
