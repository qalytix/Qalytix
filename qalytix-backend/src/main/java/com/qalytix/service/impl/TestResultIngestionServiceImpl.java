package com.qalytix.service.impl;

import com.qalytix.entity.Build;
import com.qalytix.entity.JenkinsConfig;
import com.qalytix.entity.Job;
import com.qalytix.entity.TestResult;
import com.qalytix.entity.enums.BuildStatus;
import com.qalytix.entity.enums.TestStatus;
import com.qalytix.integration.cucumber.CucumberJsonParser;
import com.qalytix.integration.jenkins.JenkinsArtifactInfo;
import com.qalytix.integration.jenkins.JenkinsClient;
import com.qalytix.integration.jenkins.JenkinsTestReportResponse;
import com.qalytix.integration.junit.JUnitXmlParser;
import com.qalytix.integration.junit.ParsedTestCase;
import com.qalytix.repository.TestResultRepository;
import com.qalytix.service.TestResultIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestResultIngestionServiceImpl implements TestResultIngestionService {

    private final JenkinsClient        jenkinsClient;
    private final JUnitXmlParser       jUnitXmlParser;
    private final CucumberJsonParser   cucumberJsonParser;
    private final TestResultRepository testResultRepository;

    @Override
    @Transactional
    public void ingestForBuild(JenkinsConfig config, Job job, Build build) {
        if (build.getStatus() == BuildStatus.IN_PROGRESS) return;
        if (testResultRepository.existsByBuildId(build.getId())) return;

        List<ParsedTestCase> cases = fetchViaTestReportApi(config, job, build);

        // Fall back to scanning archived XML artifacts
        if (cases.isEmpty()) {
            cases = fetchViaXmlArtifacts(config, job, build);
        }

        // Fall back to Cucumber JSON artifacts
        if (cases.isEmpty()) {
            cases = fetchViaCucumberJson(config, job, build);
        }

        if (cases.isEmpty()) {
            log.debug("No test results found for build #{} of job [{}]",
                    build.getBuildNumber(), job.getJenkinsJobName());
            return;
        }

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
        log.info("Ingested {} test results for build #{} of job [{}]",
                results.size(), build.getBuildNumber(), job.getJenkinsJobName());
    }

    private List<ParsedTestCase> fetchViaTestReportApi(JenkinsConfig config, Job job, Build build) {
        JenkinsTestReportResponse report = jenkinsClient.fetchTestReport(
                config, job.getJenkinsJobName(), build.getBuildNumber());
        if (report == null || report.suites() == null) return List.of();

        List<ParsedTestCase> cases = new ArrayList<>();
        for (JenkinsTestReportResponse.Suite suite : report.suites()) {
            if (suite.cases() == null) continue;
            String suiteName = suite.name() != null ? suite.name() : "Unknown";
            for (JenkinsTestReportResponse.Case tc : suite.cases()) {
                TestStatus status = mapStatus(tc.status());
                long durationMs = Math.round(tc.duration() * 1000);
                cases.add(new ParsedTestCase(suiteName, tc.name(), status, durationMs, tc.errorDetails()));
            }
        }
        log.debug("Test report API returned {} cases for build #{} of job [{}]",
                cases.size(), build.getBuildNumber(), job.getJenkinsJobName());
        return cases;
    }

    private List<ParsedTestCase> fetchViaXmlArtifacts(JenkinsConfig config, Job job, Build build) {
        List<JenkinsArtifactInfo> artifacts = jenkinsClient.fetchArtifacts(
                config, job.getJenkinsJobName(), build.getBuildNumber());

        List<JenkinsArtifactInfo> xmlArtifacts = artifacts.stream()
                .filter(a -> a.fileName().endsWith(".xml") && isTestReport(a.relativePath()))
                .toList();

        if (xmlArtifacts.isEmpty()) return List.of();

        List<ParsedTestCase> all = new ArrayList<>();
        for (JenkinsArtifactInfo artifact : xmlArtifacts) {
            String xml = jenkinsClient.downloadArtifact(
                    config, job.getJenkinsJobName(), build.getBuildNumber(), artifact.relativePath());
            if (xml != null) all.addAll(jUnitXmlParser.parse(xml));
        }
        return all;
    }

    private TestStatus mapStatus(String jenkinsStatus) {
        if (jenkinsStatus == null) return TestStatus.ERROR;
        return switch (jenkinsStatus.toUpperCase()) {
            case "PASSED", "FIXED"   -> TestStatus.PASSED;
            case "FAILED", "REGRESSION" -> TestStatus.FAILED;
            case "SKIPPED"           -> TestStatus.SKIPPED;
            default                  -> TestStatus.ERROR;
        };
    }

    private boolean isTestReport(String path) {
        String lower = path.toLowerCase();
        return lower.contains("surefire") || lower.contains("test-result")
                || lower.contains("test_result") || lower.contains("reports");
    }

    private List<ParsedTestCase> fetchViaCucumberJson(JenkinsConfig config, Job job, Build build) {
        List<JenkinsArtifactInfo> artifacts = jenkinsClient.fetchArtifacts(
                config, job.getJenkinsJobName(), build.getBuildNumber());

        List<ParsedTestCase> all = new ArrayList<>();
        for (JenkinsArtifactInfo artifact : artifacts) {
            if (!artifact.fileName().endsWith(".json")) continue;
            String json = jenkinsClient.downloadArtifact(
                    config, job.getJenkinsJobName(), build.getBuildNumber(), artifact.relativePath());
            if (json != null && cucumberJsonParser.isCucumberJson(json)) {
                log.debug("Parsing Cucumber JSON artifact {} for build #{} of job [{}]",
                        artifact.relativePath(), build.getBuildNumber(), job.getJenkinsJobName());
                all.addAll(cucumberJsonParser.parse(json));
            }
        }
        return all;
    }
}
