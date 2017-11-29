package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

public interface BrokerSink {
	void create(Context ctx);
	void getLastOperation(Context ctx);
	void bind(Context ctx);
	void delete(Context ctx);
	void unBind(Context ctx);
}
