package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import org.junit.Test;
import org.mockito.Mockito;

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
    public void rejects_request_with_conflicting_module_name() {
        TerraformRepository terraformRepository = Mockito.mock(TerraformRepository.class);
        //Given a list of current modules


        //When a new module is requested to be added
        TerraformModule requestedModule = aTfModule();

        //Then it checks if a conflicting module exists, and rejects the request
        TerraformModule existing = terraformRepository.getByModuleName(requestedModule.getModuleName());
    }

    private ImmutableTerraformModule aTfModule() {
        return ImmutableTerraformModule.builder().moduleName("module1").source("path/to/module").build();
    }

    @Test
    public void writes_terraform_module_invocation() {
        TerraformRepository  terraformRepository = Mockito.mock(TerraformRepository.class);

        //When a new module is requested to be added
        TerraformModule requestedModule = aTfModule();

        //It is saved
        terraformRepository.save(requestedModule);
    }

    @Test
    public void reads_terraform_state_for_completion() {

    }

}