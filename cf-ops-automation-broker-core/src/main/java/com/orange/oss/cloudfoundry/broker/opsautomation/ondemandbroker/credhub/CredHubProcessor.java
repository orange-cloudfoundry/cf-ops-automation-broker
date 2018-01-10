package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class CredHubProcessor extends DefaultBrokerProcessor {

    private static Logger logger=LoggerFactory.getLogger(CredHubProcessor.class.getName());
    private String apiUriBase;
    private CredHubConnector credHubConnector;

    public CredHubProcessor(CredHubConnector pCredHubConnector) {
        this.credHubConnector=pCredHubConnector;
    }

    @Override
    public void preBind(Context ctx) {
        //Read Credhub deployment path from context
        String path = (String)ctx.contextKeys.get(CredHubProcessorContext.secretsPath);

        //Retrieve all password credentials under the given path
        Map hm = this.credHubConnector.getAllDeploymentTree(path);

        //Write credentials into context
        String brokerPassword = (String)hm.get(CredHubProcessorContext.brokerPasswordName);
        ctx.contextKeys.put(CredHubProcessorContext.brokerPasswordValue.toString(), brokerPassword);
    }

}


