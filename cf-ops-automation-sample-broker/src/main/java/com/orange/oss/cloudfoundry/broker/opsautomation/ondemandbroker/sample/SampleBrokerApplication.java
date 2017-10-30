package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.sample;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.BrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerSink;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.ProcessorChain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class SampleBrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleBrokerApplication.class, args);
    }

    @Bean
    public ProcessorChain processorChain() {
        List<BrokerProcessor> processors=new ArrayList<BrokerProcessor>();
        processors.add(new DefaultBrokerProcessor());
        DefaultBrokerSink sink=new DefaultBrokerSink();
        ProcessorChain chain=new ProcessorChain(processors, sink);
        return chain;
    }



}
