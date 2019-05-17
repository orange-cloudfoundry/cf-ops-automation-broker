package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.DefaultBrokerProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.credhub.core.CredHubTemplate;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.password.PasswordCredential;
import org.springframework.credhub.support.password.PasswordParameters;
import org.springframework.credhub.support.password.PasswordParametersRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

public class PasswordGenProcessor extends DefaultBrokerProcessor {

	private static Logger logger=LoggerFactory.getLogger(PasswordGenProcessor.class.getName());
	
	private String instanceGroupName;
	private String propertyName;

	private String apiUriBase;
	
	/**
	 * Constructor
	 * @param instanceGroupName(à
	 * @param propertyName
	 */
	public PasswordGenProcessor(String apiUriBase, String instanceGroupName, String propertyName) {
		this.apiUriBase=apiUriBase;
		this.instanceGroupName=instanceGroupName;
		this.propertyName=propertyName;
	}

	
	@Override
	public void preCreate(Context ctx) {
		//generate password with credhub



		CredentialName credentialParam=new SimpleCredentialName("director","deployment","instance-group");
		PasswordParameters parameters=new PasswordParameters(20, true, false, true, false);
		PasswordParametersRequest parametersRequest=PasswordParametersRequest.builder()
				.name(credentialParam)
				.parameters(parameters)
				.overwrite(true).build();
		this.template().credentials().generate(parametersRequest);


		SimpleCredentialName credentialName = new SimpleCredentialName("director", "deployment", ",instance-group-property");
		CredentialDetails<PasswordCredential> passwords=this.template().credentials().getByName(credentialName, PasswordCredential.class);
		String password= passwords.getValue().getPassword();
		logger.debug("generated password {}",password);
		
		
	}


	@Override
	public void postDelete(Context ctx) {
		// TODO delete password from credhub

	}


	/**
	 * generates crehub template
	 * @return
	 */
	private CredHubTemplate template() {

		ClientHttpRequestFactory clientHttpRequestFactory=new SimpleClientHttpRequestFactory();
		org.springframework.credhub.core.CredHubProperties credHubProperties = new org.springframework.credhub.core.CredHubProperties();
		credHubProperties.setUrl(this.apiUriBase);
		CredHubTemplate template=new CredHubTemplate(credHubProperties, clientHttpRequestFactory);
		return template;
	}

	
}


