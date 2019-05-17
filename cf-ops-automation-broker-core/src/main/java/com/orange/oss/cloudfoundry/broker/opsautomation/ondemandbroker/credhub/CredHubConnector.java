package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.credhub.core.CredHubProperties;
import org.springframework.credhub.core.CredHubTemplate;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.CredentialSummary;
import org.springframework.credhub.support.password.PasswordCredential;
import org.springframework.credhub.support.value.ValueCredential;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CredHubConnector {

    private static Logger logger=LoggerFactory.getLogger(CredHubConnector.class.getName());
    private String apiUriBase;

    public CredHubConnector(String apiUriBase) {
        this.apiUriBase=apiUriBase;
    }

    public Map getAllDeploymentTree(String path) {
        //----------------------------------------
		/*credhub find --path /bosh-ops/cassandra
		- name: /bosh-ops/cassandra/cassandra_key_store_pass
		version_created_at: 2017-11-15T13:56:32Z
		- name: /bosh-ops/cassandra/cassandra_admin_password
		version_created_at: 2017-11-15T13:56:32Z
		*/
        //credhub find --path /secrets/cloudfoundry_service_brokers_cassandra_password
        //Only broker password, no ?
        //----------------------------------------
        Map<String, String> hm = new HashMap<String, String>();
        List<CredentialSummary> csList=this.template().credentials().findByPath(path);
        for (CredentialSummary cs : csList) {
            CredentialName credentialName = cs.getName();
            //Password credentials
            CredentialDetails<PasswordCredential> cdp=this.template().credentials().getByName(credentialName, PasswordCredential.class);
            String credentialValue =  cdp.getValue().getPassword();
            hm.put(credentialName.getName(), credentialValue);

            //Value credentials
            CredentialDetails<ValueCredential> cdv=this.template().credentials().getByName(credentialName, ValueCredential.class);
            credentialValue =  cdv.getValue().getValue();
            hm.put(credentialName.getName(), credentialValue);
        }
        return hm;
    }

    public void setAllDeploymentTree(String path, Map map) {
        //Usefull for what ?
    }

    public CredHubTemplate template() {
        ClientHttpRequestFactory clientHttpRequestFactory=new SimpleClientHttpRequestFactory();
        CredHubProperties credHubProperties = new CredHubProperties();
        credHubProperties.setUrl(this.apiUriBase);
        CredHubTemplate credHubTemplate = new CredHubTemplate(credHubProperties, clientHttpRequestFactory);
        return credHubTemplate;
    }

}


