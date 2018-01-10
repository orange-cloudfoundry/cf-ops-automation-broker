package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.credhub;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.credhub.core.CredHubTemplate;
import org.springframework.credhub.support.*;
import org.springframework.credhub.support.password.PasswordCredential;
import org.springframework.credhub.support.value.ValueCredential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CredHubConnectorTest {

    private static final Logger logger = LoggerFactory.getLogger(CredHubConnectorTest.class);

    @Mock
    CredHubConnector credHubConnector;

    @Mock
    CredHubTemplate credHubTemplate;

    @Test
    public void testGetAllDeploymentTree(){

        //Given data
        String path = "/bosh-ondemand/cassandra";

        SimpleCredentialName scn1 = new SimpleCredentialName("bosh-ondemand", "cassandra", "cassandra_key_store_pass");
        CredentialSummary cs = new CredentialSummary(scn1);
        List<CredentialSummary> csList = new ArrayList<CredentialSummary>();
        csList.add(cs);
        PasswordCredential pc = new PasswordCredential("key_store_pass");
        CredentialDetails<PasswordCredential> cdp = new CredentialDetails<PasswordCredential>("1", scn1, CredentialType.PASSWORD, pc);
        List<CredentialDetails<PasswordCredential>> cdpList = new ArrayList<CredentialDetails<PasswordCredential>>();
        cdpList.add(cdp);

        SimpleCredentialName scn2 = new SimpleCredentialName("bosh-ondemand", "cassandra", "cassandra_admin_password");
        cs = new CredentialSummary(scn2);
        csList.add(cs);
        ValueCredential vc = new ValueCredential("admin_pass");
        CredentialDetails<ValueCredential> cdv = new CredentialDetails<ValueCredential>("1", scn2, CredentialType.VALUE, vc);
        List<CredentialDetails<ValueCredential>> cdvList = new ArrayList<CredentialDetails<ValueCredential>>();
        cdvList.add(cdv);

        //Given behaviour
        when (credHubConnector.template()).thenReturn(credHubTemplate);
        when (credHubConnector.getAllDeploymentTree(path)).thenCallRealMethod();
        when (credHubTemplate.findByPath(path)).thenReturn(csList);
        when (credHubTemplate.getByName(isA(CredentialName.class), eq(PasswordCredential.class))).thenReturn(cdpList);
        when (credHubTemplate.getByName(isA(CredentialName.class), eq(ValueCredential.class))).thenReturn(cdvList);

        //When
        Map actual = credHubConnector.getAllDeploymentTree(path);

        //Then
        assertTrue(actual.containsKey("/bosh-ondemand/cassandra/cassandra_key_store_pass"));
        assertTrue(actual.containsKey("/bosh-ondemand/cassandra/cassandra_admin_password"));
    }

}
