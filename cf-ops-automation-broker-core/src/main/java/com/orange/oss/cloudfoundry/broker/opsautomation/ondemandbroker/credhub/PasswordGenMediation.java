package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import java.util.List;

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

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.mediations.DefaultBrokerMediation;

public class PasswordGenMediation extends DefaultBrokerMediation {

	private static Logger logger=LoggerFactory.getLogger(PasswordGenMediation.class.getName());
	
	private String instanceGroupName;
	private String propertyName;

	private String apiUriBase;
	
	/**
	 * Constructor
	 * @param instanceGroupName(à
	 * @param propertyName
	 */
	public PasswordGenMediation(String apiUriBase,String instanceGroupName,String propertyName) {
		this.apiUriBase=apiUriBase;
		this.instanceGroupName=instanceGroupName;
		this.propertyName=propertyName;
	}

	
	@Override
	public void preCreate(Context ctx) {
		//generate password with credhub
		

		String cred="director/deployment/instance-group-property";
		
		CredentialName credentialParam=new SimpleCredentialName("director","deployment","instance-group");
		PasswordParameters parameters=new PasswordParameters(20, true, false, true, false);
		PasswordParametersRequest parametersRequest=PasswordParametersRequest.builder()
				.name(credentialParam)
				.parameters(parameters)
				.overwrite(true).build();
		this.template().generate(parametersRequest);
		
		
		List<CredentialDetails<PasswordCredential>> passwords=this.template().getByName(cred, PasswordCredential.class);
		PasswordCredential generatedCredential = passwords.get(0).getValue();
		String password=generatedCredential.getPassword();
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

		CredHubTemplate template=new CredHubTemplate(this.apiUriBase, clientHttpRequestFactory);
		return template;
	}
	
	
	
}


