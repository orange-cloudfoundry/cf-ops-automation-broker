package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.GitClient;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GitClientTests {

	@Autowired
	GitClient gitClient;
	
	@Test
	public void testAddPipeline() throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		this.gitClient.addPipeline("testPipeline");
	}

}
