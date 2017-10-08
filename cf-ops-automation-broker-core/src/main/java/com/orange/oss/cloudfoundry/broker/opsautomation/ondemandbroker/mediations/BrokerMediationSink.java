package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;

public interface BrokerMediationSink  {
	public void create(Context ctx);
	public void bind(Context ctx);
	public void delete(Context ctx);
	public void unBind(Context ctx);
}
