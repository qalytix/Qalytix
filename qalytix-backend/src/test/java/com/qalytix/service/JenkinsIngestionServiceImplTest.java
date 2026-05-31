package com.qalytix.service;

import com.qalytix.entity.Build;
import com.qalytix.entity.Job;
import com.qalytix.entity.JenkinsConfig;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.integration.jenkins.JenkinsBuildInfo;
import com.qalytix.integration.jenkins.JenkinsClient;
import com.qalytix.integration.jenkins.JenkinsJobInfo;
import com.qalytix.integration.notification.NotificationDispatcher;
import com.qalytix.repository.BuildRepository;
import com.qalytix.repository.JenkinsConfigRepository;
import com.qalytix.repository.JobRepository;
import com.qalytix.service.impl.JenkinsIngestionServiceImpl;
import com.qalytix.websocket.BuildEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JenkinsIngestionServiceImplTest {

    @Mock JenkinsClient                  jenkinsClient;
    @Mock JenkinsConfigRepository        jenkinsConfigRepository;
    @Mock JobRepository                  jobRepository;
    @Mock BuildRepository                buildRepository;
    @Mock BuildEventPublisher            buildEventPublisher;
    @Mock TestResultIngestionService     testResultIngestionService;
    @Mock NotificationDispatcher         notificationDispatcher;

    @InjectMocks JenkinsIngestionServiceImpl service;

    private JenkinsConfig config() {
        return JenkinsConfig.builder()
                .id(1L).orgId(10L).name("CI").url("http://jenkins")
                .username("admin").apiToken("token").pollInterval("*/5 * * * *")
                .build();
    }

    private JenkinsBuildInfo buildInfo(boolean building, String result) {
        return new JenkinsBuildInfo(42, result, 5000L, System.currentTimeMillis(), "http://j/42", building, "user1");
    }

    @Test
    void syncConfig_createsNewJobAndBuild() {
        var cfg = config();
        var buildInfo = buildInfo(false, "SUCCESS");
        var jobInfo = new JenkinsJobInfo("my-job", "My Job", "http://j/job/my-job", buildInfo, null);

        when(jenkinsClient.fetchJobs(cfg)).thenReturn(List.of(jobInfo));
        when(jobRepository.findByJenkinsConfigIdAndJenkinsJobName(1L, "my-job")).thenReturn(Optional.empty());
        var savedJob = Job.builder().id(100L).orgId(10L).jenkinsConfigId(1L).jenkinsJobName("my-job").build();
        when(jobRepository.save(any())).thenReturn(savedJob);
        when(buildRepository.findByJobIdAndBuildNumber(100L, 42)).thenReturn(Optional.empty());
        when(buildRepository.save(any())).thenReturn(Build.builder().id(200L).build());

        service.syncConfig(cfg);

        verify(jobRepository).save(argThat(j -> j.getJenkinsJobName().equals("my-job")));
        verify(buildRepository).save(argThat(b -> b.getBuildNumber() == 42 && b.getStatus() == BuildStatus.SUCCESS));
        verify(jenkinsConfigRepository).save(argThat(c -> c.getLastSyncedAt() != null));
    }

    @Test
    void syncConfig_updatesExistingJobAndBuild() {
        var cfg = config();
        var buildInfo = buildInfo(false, "FAILURE");
        var jobInfo = new JenkinsJobInfo("my-job", "My Job", "http://j/job/my-job", buildInfo, null);

        var existingJob = Job.builder().id(100L).orgId(10L).jenkinsConfigId(1L).jenkinsJobName("my-job").build();
        when(jenkinsClient.fetchJobs(cfg)).thenReturn(List.of(jobInfo));
        when(jobRepository.findByJenkinsConfigIdAndJenkinsJobName(1L, "my-job")).thenReturn(Optional.of(existingJob));
        when(jobRepository.save(any())).thenReturn(existingJob);

        var existingBuild = Build.builder().id(200L).jobId(100L).buildNumber(42).status(BuildStatus.IN_PROGRESS).build();
        when(buildRepository.findByJobIdAndBuildNumber(100L, 42)).thenReturn(Optional.of(existingBuild));
        when(buildRepository.save(any())).thenReturn(existingBuild);

        service.syncConfig(cfg);

        verify(buildRepository).save(argThat(b -> b.getStatus() == BuildStatus.FAILURE));
    }

    @Test
    void syncConfig_inProgressBuildStatusMappedCorrectly() {
        var cfg = config();
        var buildInfo = buildInfo(true, null);
        var jobInfo = new JenkinsJobInfo("my-job", "My Job", "http://j", buildInfo, null);

        when(jenkinsClient.fetchJobs(cfg)).thenReturn(List.of(jobInfo));
        when(jobRepository.findByJenkinsConfigIdAndJenkinsJobName(1L, "my-job")).thenReturn(Optional.empty());
        var savedJob = Job.builder().id(100L).orgId(10L).build();
        when(jobRepository.save(any())).thenReturn(savedJob);
        when(buildRepository.findByJobIdAndBuildNumber(anyLong(), eq(42))).thenReturn(Optional.empty());
        when(buildRepository.save(any())).thenReturn(Build.builder().build());

        service.syncConfig(cfg);

        verify(buildRepository).save(argThat(b -> b.getStatus() == BuildStatus.IN_PROGRESS));
    }

    @Test
    void syncConfig_skipsJobWithNoBuild() {
        var cfg = config();
        var jobInfo = new JenkinsJobInfo("my-job", "My Job", "http://j", null, null);

        when(jenkinsClient.fetchJobs(cfg)).thenReturn(List.of(jobInfo));
        when(jobRepository.findByJenkinsConfigIdAndJenkinsJobName(1L, "my-job")).thenReturn(Optional.empty());
        when(jobRepository.save(any())).thenReturn(Job.builder().id(100L).build());

        service.syncConfig(cfg);

        verifyNoInteractions(buildRepository);
    }

    @Test
    void syncAll_continuesOnConfigError() {
        var cfg1 = JenkinsConfig.builder().id(1L).orgId(10L).name("C1").url("http://j1").username("u").apiToken("t").pollInterval("*/5 * * * *").build();
        var cfg2 = JenkinsConfig.builder().id(2L).orgId(10L).name("C2").url("http://j2").username("u").apiToken("t").pollInterval("*/5 * * * *").build();

        when(jenkinsConfigRepository.findAllByActiveTrue()).thenReturn(List.of(cfg1, cfg2));
        when(jenkinsClient.fetchJobs(cfg1)).thenThrow(new RuntimeException("unreachable"));
        when(jenkinsClient.fetchJobs(cfg2)).thenReturn(List.of());

        service.syncAll();

        // cfg2 still processed; no exception propagated
        verify(jenkinsClient).fetchJobs(cfg1);
        verify(jenkinsClient).fetchJobs(cfg2);
    }
}
