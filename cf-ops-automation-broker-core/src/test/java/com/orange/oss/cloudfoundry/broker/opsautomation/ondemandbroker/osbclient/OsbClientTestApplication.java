package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.osbclient;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline.OsbBuilderHelper;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@SpringBootApplication
public class OsbClientTestApplication {

    @Bean
    public Catalog enableOsbFrameworkWithCatalog() {
        return OsbBuilderHelper.aCatalog();
    }



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
