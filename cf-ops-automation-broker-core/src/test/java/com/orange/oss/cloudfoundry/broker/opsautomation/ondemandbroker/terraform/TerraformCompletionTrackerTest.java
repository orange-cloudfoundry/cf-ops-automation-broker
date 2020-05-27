package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import java.io.File;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class TerraformCompletionTrackerTest {

    @Test
    public void returns_creation_status_matching_tfstate_output() {
        //given a tfstate with a completed module
        String tfStateFileInClasspath = "/terraform/terraform-with-completed-module-exec.tfstate";
        //4567.completed = successfully provisionned /tmp/writeable/file and /tmp/writeable/file
        //4567.started = successfully received module invocation

        TerraformCompletionTracker tracker = new TerraformCompletionTracker(Clock.systemUTC(), 120, "terraform-with-completed-module-exec.tfstate");

        //when asked status
        File fileFromClasspath = getFileFromClasspath(tfStateFileInClasspath);
        GetLastServiceOperationResponse moduleExecStatus = tracker.getModuleExecStatus(fileFromClasspath.getParentFile().toPath(), "4567", "{\"lastOperationDate\":\"2017-11-14T17:24:08.007Z\",\"operation\":\"create\"}");

        //
        assertThat(moduleExecStatus.getState()).isEqualTo(OperationState.SUCCEEDED);
    }

    @Test
    public void returns_creation_failure_when_max_execution_time_reached() {
        //given a tf state with pending execution
        String tfStateFileInClasspath = "/terraform/terraform-without-completed-module-exec.tfstate";
        File tfStateFile = getFileFromClasspath(tfStateFileInClasspath);
        Path gitWorkDir = tfStateFile.getParentFile().toPath();

        //given a configured timeout
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(clock, 120, "terraform-without-completed-module-exec.tfstate");

        //when asked status before timeout
        GetLastServiceOperationResponse moduleExecStatus = tracker.getModuleExecStatus(gitWorkDir, "4567", "{\"lastOperationDate\":\"2017-11-14T17:24:08.007Z\",\"operation\":\"create\"}");
        assertThat(moduleExecStatus.getState()).isEqualTo(OperationState.IN_PROGRESS);

        //when asked status after timeout
        moduleExecStatus = tracker.getModuleExecStatus(gitWorkDir, "4567", "{\"lastOperationDate\":\"2017-11-14T17:14:08.007Z\",\"operation\":\"create\"}");//-10 mins
        assertThat(moduleExecStatus.getState()).isEqualTo(OperationState.FAILED);
        assertThat(moduleExecStatus.getDescription()).contains("timeout");
    }

    @Test
    public void returns_deletion_failure_when_max_execution_time_reached() {
        //given a tf state with pending execution
        String tfStateFileInClasspath = "/terraform/terraform-with-completed-module-exec.tfstate";
        File tfStateFile = getFileFromClasspath(tfStateFileInClasspath);
        Path gitWorkDir = tfStateFile.getParentFile().toPath();

        //given a configured timeout
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(clock, 120, "terraform-with-completed-module-exec.tfstate");

        //when asked status before timeout
        GetLastServiceOperationResponse moduleExecStatus = tracker.getModuleExecStatus(gitWorkDir, "4567", "{\"lastOperationDate\":\"2017-11-14T17:24:08.007Z\",\"operation\":\"delete\"}");
        assertThat(moduleExecStatus.getState()).isEqualTo(OperationState.IN_PROGRESS);

        //when asked status after timeout
        moduleExecStatus = tracker.getModuleExecStatus(gitWorkDir, "4567", "{\"lastOperationDate\":\"2017-11-14T17:14:08.007Z\",\"operation\":\"delete\"}");//-10 mins
        assertThat(moduleExecStatus.getState()).isEqualTo(OperationState.FAILED);
        assertThat(moduleExecStatus.getDescription()).contains("timeout");
    }

    @Test
    public void maps_tf_outputs_to_creation_status() {
        ImmutableOutput successOutput = ImmutableOutput.builder()
                .type("String")
                .value("success").build();
        assert_expected_status(null, null, OperationState.IN_PROGRESS, false);
        assert_expected_status(successOutput, null, OperationState.FAILED, false);
        assert_expected_status(successOutput, successOutput, OperationState.SUCCEEDED, false);
    }
    @Test
    public void maps_tf_outputs_to_deletion_status() {
        ImmutableOutput successOutput = ImmutableOutput.builder()
                .type("String")
                .value("success").build();
        assert_expected_status(successOutput, successOutput, OperationState.IN_PROGRESS, true);
        assert_expected_status(successOutput, null, OperationState.IN_PROGRESS, true); //before timeout
        assert_expected_status(null, successOutput, OperationState.FAILED, true);
        assert_expected_status(null, null, OperationState.SUCCEEDED, true);
    }

    @Test
    public void serializes_unserializes_state_into_json() {
        //given
        TerraformCompletionTracker.TfOperationState tfOperationState = new TerraformCompletionTracker.TfOperationState("2017-11-14T17:24:08.007Z", "create");
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(Clock.systemUTC(), 120, "terraform.tfstate");

        //when
        String json = tracker.formatAsJson(tfOperationState);
        assertThat(json).isEqualTo("{\"lastOperationDate\":\"2017-11-14T17:24:08.007Z\",\"operation\":\"create\"}");

        //and
        assertThat(tracker.parseFromJson(json)).isEqualTo(tfOperationState);
    }

    @Test
    public void provides_current_date_for_last_operation() {
        //given
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(clock, 120, "terraform.tfstate");

        //when
        String currentDateAsOperation = tracker.getCurrentDate();
        //then
        assertThat(currentDateAsOperation).isEqualTo("2017-11-14T17:24:08.007Z");

    }

    @Test
    public void get_elapsed_time_since_last_operation_and_operation_type() {
        //given
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L+120*1000L), ZoneId.of("Europe/Paris"));
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(clock, 120, "terraform.tfstate");
        //when
        long elapsedTimeSecsSinceLastOperation = tracker.getElapsedTimeSecsSinceLastOperation("2017-11-14T17:24:08.007Z");

        //then
        assertThat(elapsedTimeSecsSinceLastOperation).isEqualTo(120L);
    }

    @Test
    public void given_we_understand_date_format_and_parsing() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));

        Instant now = Instant.now(clock);
        assertThat(now.toString()).isEqualTo("2017-11-14T17:24:08.007Z");

        assertThat(Instant.parse(now.toString())).isEqualTo(now);
    }

    public static File getFileFromClasspath(String tfStateFileInClasspath) {
        String path = TerraformModuleHelper.class.getResource(tfStateFileInClasspath).getFile();
        File tfStateFile = new File(path);
        assertThat(tfStateFile).exists();
        return tfStateFile;
    }


    private void assert_expected_status(TerraformState.Output started, TerraformState.Output completed, OperationState expected, boolean isDelete) {
        File tfStateFile = Mockito.mock(File.class);
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(Clock.systemUTC(), 120, "terraform.tfstate");

        GetLastServiceOperationResponse response = isDelete ?
                tracker.mapOutputToDeletionStatus(started, completed, 5) :
                tracker.mapOutputToCreationStatus(started, completed, 5);

        assertThat(response.isDeleteOperation()).isEqualTo(isDelete);
        assertThat(response.getState()).isEqualTo(expected);
    }

}