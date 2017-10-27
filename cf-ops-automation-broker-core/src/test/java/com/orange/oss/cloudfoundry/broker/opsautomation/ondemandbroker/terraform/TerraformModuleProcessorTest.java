package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public class TerraformModuleProcessorTest {

    TerraformModuleProcessor terraformModuleProcessor = new TerraformModuleProcessor();

    @Test
    public void looks_up_paas_secrets_git_local_checkout() throws IOException {
        //given
        //the git mediation cloned the repo and populated context
        Context ctx = new Context();
        Path workDir = Files.createTempDirectory("paas-secret-clone");

        ctx.contextKeys.put(GitProcessorContext.workDir.toString(),workDir);

        //when
        //Path lookedUpWorkDir = terraformModuleProcessor.lookUpGitworkDir(ctx);

        //then
        //Asserts.assertThat(lookedUpWorkDir.equals());

    }

    @Test
    public void receives_service_instance_creation_inputs() {

    }



    @Test
    public void check_service_instance_conflicting_state() {
        TerraformRepository terraformRepository;
    }

    @Test
    public void writes_terraform_module_invocation() {

    }

    @Test
    public void reads_terraform_state_for_completion() {

    }

}