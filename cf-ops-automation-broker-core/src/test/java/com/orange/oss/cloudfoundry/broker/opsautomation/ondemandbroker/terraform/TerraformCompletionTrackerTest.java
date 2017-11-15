package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.terraform;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
public class TerraformCompletionTrackerTest {

    @Test
    public void returns_status_matching_tfstate_output() {
        //given a tfstate with a completed module
        String tfStateFileInClasspath = "/terraform/terraform-with-multiple-outputs.tfstate";
        //4567.completed = successfully provisionned /tmp/writeable/file and /tmp/writeable/file
        //4567.started = successfully received module invocation

        String path = TerraformModuleHelper.class.getResource(tfStateFileInClasspath).getFile();
        File tfStateFile = new File(path);
        assertThat(tfStateFile).exists();

        TerraformCompletionTracker tracker = new TerraformCompletionTracker(tfStateFile, Clock.systemUTC());

        //when asked status
        GetLastServiceOperationResponse moduleExecStatus = tracker.getModuleExecStatus("4567");

        //
        assertThat(moduleExecStatus.getState()).isEqualTo(OperationState.SUCCEEDED);
    }

    @Test
    public void maps_tf_outputs_to_status() {
        ImmutableOutput successOutput = ImmutableOutput.builder()
                .type("String")
                .value("success").build();
        assert_expected_status(null, null, OperationState.IN_PROGRESS);
        assert_expected_status(successOutput, null, OperationState.FAILED);
        assert_expected_status(successOutput, successOutput, OperationState.SUCCEEDED);
    }

    @Test
    public void provides_current_date_for_last_operation() {
        //given
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L), ZoneId.of("Europe/Paris"));
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(Mockito.mock(File.class),clock);

        //when
        String currentDateAsOperation = tracker.getCurrentDate();
        //then
        assertThat(currentDateAsOperation).isEqualTo("2017-11-14T17:24:08.007Z");

    }

    @Test
    public void get_elapsed_time_since_last_operation() {
        //given
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1510680248007L+120*1000L), ZoneId.of("Europe/Paris"));
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(Mockito.mock(File.class),clock);

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


    private void assert_expected_status(TerraformState.Output started, TerraformState.Output completed, OperationState expected) {
        File tfStateFile = Mockito.mock(File.class);
        TerraformCompletionTracker tracker = new TerraformCompletionTracker(tfStateFile, Clock.systemUTC());

        GetLastServiceOperationResponse response = tracker.mapOutputToStatus(started, completed);

        assertThat(response.getState()).isEqualTo(expected);
    }

}