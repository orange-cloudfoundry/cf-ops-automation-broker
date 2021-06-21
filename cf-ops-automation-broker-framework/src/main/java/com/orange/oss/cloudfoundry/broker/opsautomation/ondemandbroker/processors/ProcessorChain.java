package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;



public class ProcessorChain {

	private static Logger logger=LoggerFactory.getLogger(ProcessorChain.class.getName());
	
	private List<BrokerProcessor> defaultProcessors;
	private BrokerSink sink;
	
	public ProcessorChain(List<BrokerProcessor> defaultProcessors, BrokerSink sink) {
		this.defaultProcessors = defaultProcessors;
		this.sink=sink;
	}

	public void create(Context ctx) {
		try {
			for (BrokerProcessor m : defaultProcessors) {
				m.preCreate(ctx);
			}
			sink.create(ctx);

			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.postCreate(ctx);
			}
		} finally {
			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.cleanUp(ctx);
			}
		}
	}
	public void getInstance(Context ctx) {
		try {
			for (BrokerProcessor m : defaultProcessors) {
				m.preGetInstance(ctx);
			}
			sink.getInstance(ctx);

			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.postGetInstance(ctx);
			}
		} finally {
			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.cleanUp(ctx);
			}
		}
	}

	public void getLastOperation(Context ctx) {
		try {
			for (BrokerProcessor m: defaultProcessors) {
                m.preGetLastOperation(ctx);
            }
			sink.getLastOperation(ctx);

			for (int i = defaultProcessors.size()-1; i>=0; i--) {
                BrokerProcessor m= defaultProcessors.get(i);
				m.postGetLastOperation(ctx);
            }
		} finally {
			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.cleanUp(ctx);
			}
		}
	}

	public void bind(Context ctx) {
		try {
			for (BrokerProcessor m: defaultProcessors) {
                m.preBind(ctx);
            }
			sink.bind(ctx);

			for (int i = defaultProcessors.size()-1; i>=0; i--) {
                BrokerProcessor m= defaultProcessors.get(i);
				m.postBind(ctx);
            }
		} finally {
			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.cleanUp(ctx);
			}
		}

	}


	public void unBind(Context ctx) {
		try {
			for (BrokerProcessor m: defaultProcessors) {
                m.preUnBind(ctx);
            }
			sink.unBind(ctx);

			for (int i = defaultProcessors.size()-1; i>=0; i--) {
                BrokerProcessor m= defaultProcessors.get(i);
				m.postUnBind(ctx);
            }
		} finally {
			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.cleanUp(ctx);
			}
		}

	}

	public void update(Context ctx) {
		try {
			for (BrokerProcessor m: defaultProcessors) {
                m.preUpdate(ctx);
            }
			sink.update(ctx);

			for (int i = defaultProcessors.size()-1; i>=0; i--) {
                BrokerProcessor m= defaultProcessors.get(i);
				m.postUpdate(ctx);
            }
		} finally {
			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.cleanUp(ctx);
			}
		}
	}

	public void delete(Context ctx) {
		try {
			for (BrokerProcessor m: defaultProcessors) {
                m.preDelete(ctx);
            }
			sink.delete(ctx);

			for (int i = defaultProcessors.size()-1; i>=0; i--) {
                BrokerProcessor m= defaultProcessors.get(i);
				m.postDelete(ctx);
            }
		} finally {
			for (int i = defaultProcessors.size() - 1; i >= 0; i--) {
				BrokerProcessor m = defaultProcessors.get(i);
				m.cleanUp(ctx);
			}
		}
	}



}
