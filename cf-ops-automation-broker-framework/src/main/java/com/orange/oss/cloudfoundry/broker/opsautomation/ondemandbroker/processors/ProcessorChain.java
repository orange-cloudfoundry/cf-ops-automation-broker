package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ProcessorChain {

	private static Logger logger=LoggerFactory.getLogger(ProcessorChain.class.getName());
	
	private List<BrokerProcessor> processors;
	private BrokerSink sink;
	
	public ProcessorChain(List<BrokerProcessor> processors, BrokerSink sink) {
		this.processors = processors;
		this.sink=sink;
	}

	public void create() {
		Context ctx=new Context();
		create(ctx);

	}

	public void create(Context ctx) {
		for (BrokerProcessor m: processors) {
			m.preCreate(ctx);
		}
		sink.create(ctx);

		for (int i = processors.size()-1; i>=0; i--) {
			BrokerProcessor m= processors.get(i);
			m.postCreate(ctx);
		}
	}

	public void getLastCreateOperation() {
		Context ctx=new Context();
		getLastCreateOperation(ctx);
	}

	public void getLastCreateOperation(Context ctx) {
		for (BrokerProcessor m: processors) {
			m.preGetLastCreateOperation(ctx);
		}
		sink.getLastCreateOperation(ctx);

		for (int i = processors.size()-1; i>=0; i--) {
			BrokerProcessor m= processors.get(i);
			m.postGetLastCreateOperation(ctx);
		}
	}

	public void bind() {
		Context ctx=new Context();
		for (BrokerProcessor m: processors) {
			m.preBind(ctx);
		}
		sink.bind(ctx);

		for (int i = processors.size()-1; i>=0; i--) {
			BrokerProcessor m= processors.get(i);
			m.postBind(ctx);
		}

	}


	public void unBind() {
		Context ctx=new Context();
		for (BrokerProcessor m: processors) {
			m.preUnBind(ctx);
		}
		sink.unBind(ctx);

		for (int i = processors.size()-1; i>=0; i--) {
			BrokerProcessor m= processors.get(i);
			m.postUnBind(ctx);
		}

	}

	public void delete(Context ctx) {
		for (BrokerProcessor m: processors) {
			m.preDelete(ctx);
		}
		sink.delete(ctx);

		for (int i = processors.size()-1; i>=0; i--) {
			BrokerProcessor m= processors.get(i);
			m.postDelete(ctx);
		}

	}
}
