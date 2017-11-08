package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.git.GitProcessorContext;
import com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.processors.Context;
import com.orange.oss.ondemandbroker.ProcessorChainServiceInstanceService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TerraformModuleProcessorTest {

    @Mock
    TerraformRepository terraformRepository;
    @InjectMocks
    TerraformModuleProcessor terraformModuleProcessor;

    @Test
    public void preCreate_responds_to_tf_module_creation_requests() {
        //given a service instance request comes in
        Context context = aContextWithCreateRequest();

        //and a previous processor in the chain that inserted a tf module in the context
        ImmutableTerraformModule injectedModule = aTfModule();
        context.contextKeys.put(TerraformModuleProcessor.ADD_TF_MODULE, injectedModule);

        //when
        terraformModuleProcessor.preCreate(context);

        //then it persists the new module
        verify(terraformRepository).save(any(TerraformModule.class));
    }

    @Test
    public void receives_tf_module_creation_requests() {
        //given a service instance request comes in
        Context context = aContextWithCreateRequest();

        //and a previous processor in the chain that inserted a tf module in the context
        ImmutableTerraformModule requestedModule = aTfModule();
        context.contextKeys.put(TerraformModuleProcessor.ADD_TF_MODULE, requestedModule);

        //when
        TerraformModule module = terraformModuleProcessor.getRequestedTerraformModule(context);

        //then it assigns the module id from service instance guid
        ImmutableTerraformModule expectedTerraformModule = ImmutableTerraformModule.builder().from(requestedModule)
                .id("service-instance-guid").build();
        assertThat(module).isEqualTo(expectedTerraformModule);
    }

    @Test(expected = RuntimeException.class)
    public void rejects_request_with_conflicting_module_name() {
        //given a repository populated with an existing module
        TerraformRepository terraformRepository = Mockito.mock(TerraformRepository.class);
        when(terraformRepository.getByModuleId("service-instance-guid")).thenReturn(aTfModule());
        terraformModuleProcessor = new TerraformModuleProcessor(terraformRepository);

        //given a service instance request comes in
        Context context = aContextWithCreateRequest();

        //When a new module is requested to be added
        // by a previous processor in the chain that inserted a tf module in the context
        TerraformModule requestedModule = ImmutableTerraformModule.builder().from(aTfModule())
                .id("service-instance-guid")
                .build();
        context.contextKeys.put(TerraformModuleProcessor.ADD_TF_MODULE, requestedModule);


        //Then it checks if a conflicting module exists, and rejects the request
        //when
        terraformModuleProcessor.preCreate(context);
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


    private ImmutableTerraformModule aTfModule() {
        return ImmutableTerraformModule.builder()
                .moduleName("module1")
                .source("path/to/module").build();
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