package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

public interface BrokerProcessor {

	void preCreate(Context ctx);
	void postCreate(Context ctx);

	void preGetLastOperation(Context ctx);
	void postGetLastOperation(Context ctx);

	void preBind(Context ctx);
	void postBind(Context ctx);

	void preUnBind(Context ctx);
	void postUnBind(Context ctx);

	void preUpdate(Context ctx);
	void postUpdate(Context ctx);

	void preDelete(Context ctx);
	void postDelete(Context ctx);

	void cleanUp(Context ctx);
}
