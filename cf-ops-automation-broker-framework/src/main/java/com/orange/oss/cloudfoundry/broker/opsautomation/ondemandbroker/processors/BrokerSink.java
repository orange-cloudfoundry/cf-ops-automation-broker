package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

public interface BrokerSink {
	public void create(Context ctx);
	public void getLastCreateOperation(Context ctx);
	public void bind(Context ctx);
	public void delete(Context ctx);
	public void unBind(Context ctx);
}
