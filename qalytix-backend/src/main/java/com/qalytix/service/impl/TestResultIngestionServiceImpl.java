package com.qalytix.service.impl;

import com.qalytix.entity.Build;
import com.qalytix.entity.JenkinsConfig;
import com.qalytix.entity.Job;
import com.qalytix.entity.TestResult;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.integration.jenkins.JenkinsArtifactInfo;
import com.qalytix.integration.jenkins.JenkinsClient;
import com.qalytix.integration.junit.JUnitXmlParser;
import com.qalytix.integration.junit.ParsedTestCase;
import com.qalytix.repository.TestResultRepository;
import com.qalytix.service.TestResultIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestResultIngestionServiceImpl implements TestResultIngestionService {

    private final JenkinsClient        jenkinsClient;
    private final JUnitXmlParser       jUnitXmlParser;
    private final TestResultRepository testResultRepository;

    @Override
    @Transactional
    public void ingestForBuild(JenkinsConfig config, Job job, Build build) {
        if (build.getStatus() == BuildStatus.IN_PROGRESS) return;
        if (testResultRepository.existsByBuildId(build.getId())) return;

        List<JenkinsArtifactInfo> artifacts = jenkinsClient.fetchArtifacts(
                config, job.getJenkinsJobName(), build.getBuildNumber());

        List<JenkinsArtifactInfo> xmlArtifacts = artifacts.stream()
                .filter(a -> a.fileName().endsWith(".xml") && isTestReport(a.relativePath()))
                .toList();

        if (xmlArtifacts.isEmpty()) {
            log.debug("No JUnit XML artifacts found for build #{} of job [{}]",
                    build.getBuildNumber(), job.getJenkinsJobName());
            return;
        }

        for (JenkinsArtifactInfo artifact : xmlArtifacts) {
            String xml = jenkinsClient.downloadArtifact(
                    config, job.getJenkinsJobName(), build.getBuildNumber(), artifact.relativePath());
            if (xml == null) continue;

            List<ParsedTestCase> cases = jUnitXmlParser.parse(xml);
            List<TestResult> results = cases.stream()
                    .map(tc -> TestResult.builder()
                            .orgId(config.getOrgId())
                            .buildId(build.getId())
                            .jobId(job.getId())
                            .testSuite(tc.testSuite())
                            .testName(tc.testName())
                            .status(tc.status())
                            .durationMs(tc.durationMs())
                            .failureMessage(tc.failureMessage())
                            .build())
                    .toList();

            testResultRepository.saveAll(results);
            log.debug("Ingested {} test results from {} for build #{}",
                    results.size(), artifact.fileName(), build.getBuildNumber());
        }
    }

    private boolean isTestReport(String path) {
        String lower = path.toLowerCase();
        return lower.contains("surefire") || lower.contains("test-result")
                || lower.contains("test_result") || lower.contains("reports");
    }
}
