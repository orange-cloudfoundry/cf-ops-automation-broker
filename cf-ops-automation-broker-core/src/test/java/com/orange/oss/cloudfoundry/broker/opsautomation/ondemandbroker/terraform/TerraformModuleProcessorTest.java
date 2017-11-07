package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform.cloudflare.CloudFlareProcessorTest;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class TerraformModuleProcessorTest {

    TerraformModuleProcessor terraformModuleProcessor = new TerraformModuleProcessor();

    @Test
    public void receives_tf_module_creation_requests() {
        //given a service instance request comes in
        Context context = aContextWithCreateRequest();

        //and a previous processor in the chain that inserted a tf module in the context
        ImmutableTerraformModule injectedModule = aTerraformModule();
        context.contextKeys.put(TerraformModuleProcessor.ADD_TF_MODULE_WITH_ID,
                injectedModule);

        //when
        TerraformModule module = terraformModuleProcessor.getRequestedTerraformModule(context);

        //then it assigns the module id from service instance guid
        ImmutableTerraformModule expectedTerraformModule = ImmutableTerraformModule.builder().from(injectedModule)
                .id("service-instance-guid").build();
        assertThat(module).isEqualTo(expectedTerraformModule);
    }

    public ImmutableTerraformModule aTerraformModule() {
        return ImmutableTerraformModule.builder()
                    .moduleName("module-name")
                    .source("path/to/module")
                    .putProperties("route-prefix", "avalidroute")
                    .build();
    }

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


    Context aContextWithCreateRequest() {

        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest();
        request.withServiceInstanceId("service-instance-guid");

        //and the context being injected to a cloudflare processor
        Context context = new Context();
        context.contextKeys.put(ProcessorChainServiceInstanceService.CREATE_SERVICE_INSTANCE_REQUEST, request);
        return context;
    }

}