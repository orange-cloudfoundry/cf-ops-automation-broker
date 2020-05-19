package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.credhub.core.CredHubTemplate;
import org.springframework.credhub.core.credential.CredHubCredentialOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.CredentialSummary;
import org.springframework.credhub.support.CredentialType;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.password.PasswordCredential;
import org.springframework.credhub.support.value.ValueCredential;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class CredHubConnectorTest {

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(CredHubConnectorTest.class);

    @Mock
    private
    CredHubConnector credHubConnector;

    @Mock
    private
    CredHubTemplate credHubTemplate;
    @Mock
    private
    CredHubCredentialOperations credentials;

    @Test
    public void testGetAllDeploymentTree(){

        //Given data
        String path = "/bosh-ondemand/cassandra";

        SimpleCredentialName scn1 = new SimpleCredentialName("bosh-ondemand", "cassandra", "cassandra_key_store_pass");
        CredentialSummary cs = new CredentialSummary(scn1);
        List<CredentialSummary> csList = new ArrayList<>();
        csList.add(cs);
        PasswordCredential pc = new PasswordCredential("key_store_pass");
        CredentialDetails<PasswordCredential> cdp = new CredentialDetails<>("1", scn1, CredentialType.PASSWORD, pc);

        SimpleCredentialName scn2 = new SimpleCredentialName("bosh-ondemand", "cassandra", "cassandra_admin_password");
        cs = new CredentialSummary(scn2);
        csList.add(cs);
        ValueCredential vc = new ValueCredential("admin_pass");
        CredentialDetails<ValueCredential> cdv = new CredentialDetails<>("1", scn2, CredentialType.VALUE, vc);

        //Given behaviour
        when (credHubConnector.template()).thenReturn(credHubTemplate);
        when (credHubConnector.getAllDeploymentTree(path)).thenCallRealMethod();
        when (credHubTemplate.credentials()).thenReturn(credentials);
        when (credentials.findByPath(path)).thenReturn(csList);
        when (credentials.getByName(isA(CredentialName.class), eq(PasswordCredential.class))).thenReturn(cdp);
        when (credentials.getByName(isA(CredentialName.class), eq(ValueCredential.class))).thenReturn(cdv);

        //When
        //noinspection rawtypes
        Map actual = credHubConnector.getAllDeploymentTree(path);

        //Then
        assertTrue(actual.containsKey("/bosh-ondemand/cassandra/cassandra_key_store_pass"));
        assertTrue(actual.containsKey("/bosh-ondemand/cassandra/cassandra_admin_password"));
    }

}
