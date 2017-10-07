package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations;

public interface BrokerMediationSink  {
	public void create();
	public void bind();
	public void delete();
	public void unBind();
}
