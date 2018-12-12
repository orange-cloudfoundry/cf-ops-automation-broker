package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.ArrayList;

/**
 *
 */
@SpringBootApplication
public class OsbClientTestApplication {

    //Note: may need to invoke BrokerCatalogConfig to instanciate expected default catalog

    public static void main(String[] args) {
        SpringApplication.run(OsbClientTestApplication.class, args);
    }

    @Bean
    public ProcessorChain processorChain() {
        List<BrokerProcessor> processors = new ArrayList<>();

        DefaultBrokerSink sink = new DefaultBrokerSink();
        return new ProcessorChain(processors, sink);
    }

}
