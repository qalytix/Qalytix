package com.qalytix.integration.jenkins;

import com.qalytix.entity.JenkinsConfig;
import com.qalytix.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
public class JenkinsClient {

    private static final String JOBS_TREE =
            "jobs[name,displayName,url,lastBuild[number,result,duration,timestamp,url,building]]";

    private static final String BUILD_TREE =
            "number,result,duration,timestamp,url,building,culprits[fullName]";

    public List<JenkinsJobInfo> fetchJobs(JenkinsConfig config) {
        try {
            JenkinsJobsResponse response = client(config)
                    .get()
                    .uri("/api/json?tree=" + JOBS_TREE)
                    .retrieve()
                    .body(JenkinsJobsResponse.class);
            return response != null && response.jobs() != null ? response.jobs() : List.of();
        } catch (RestClientException e) {
            log.error("Failed to fetch jobs from Jenkins [{}]: {}", config.getUrl(), e.getMessage());
            throw new BadRequestException("Could not reach Jenkins server: " + e.getMessage());
        }
    }

    public JenkinsBuildInfo fetchBuild(JenkinsConfig config, String jobName, int buildNumber) {
        try {
            return client(config)
                    .get()
                    .uri("/job/{job}/{build}/api/json?tree=" + BUILD_TREE, jobName, buildNumber)
                    .retrieve()
                    .body(JenkinsBuildInfo.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch build #{} for job [{}]: {}", buildNumber, jobName, e.getMessage());
            return null;
        }
    }

    public List<JenkinsArtifactInfo> fetchArtifacts(JenkinsConfig config, String jobName, int buildNumber) {
        try {
            JenkinsArtifactsResponse response = client(config)
                    .get()
                    .uri("/job/{job}/{build}/api/json?tree=artifacts[fileName,relativePath]", jobName, buildNumber)
                    .retrieve()
                    .body(JenkinsArtifactsResponse.class);
            return response != null && response.artifacts() != null ? response.artifacts() : List.of();
        } catch (RestClientException e) {
            log.warn("Failed to fetch artifacts for {}/{}: {}", jobName, buildNumber, e.getMessage());
            return List.of();
        }
    }

    public String downloadArtifact(JenkinsConfig config, String jobName, int buildNumber, String relativePath) {
        try {
            return client(config)
                    .get()
                    .uri("/job/{job}/{build}/artifact/{path}", jobName, buildNumber, relativePath)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.warn("Failed to download artifact {}: {}", relativePath, e.getMessage());
            return null;
        }
    }

    public void testConnection(JenkinsConfig config) {
        try {
            var status = client(config)
                    .get()
                    .uri("/api/json?tree=nodeName")
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            if (!status.is2xxSuccessful()) {
                throw new BadRequestException("Jenkins returned HTTP " + status.value());
            }
        } catch (RestClientException e) {
            throw new BadRequestException("Cannot connect to Jenkins: " + e.getMessage());
        }
    }

    private RestClient client(JenkinsConfig config) {
        String credentials = config.getUsername() + ":" + config.getApiToken();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return RestClient.builder()
                .baseUrl(config.getUrl())
                .defaultHeader("Authorization", "Basic " + encoded)
                .defaultStatusHandler(status -> status == HttpStatus.UNAUTHORIZED,
                        (req, res) -> { throw new BadRequestException("Jenkins credentials are invalid (401)."); })
                .build();
    }
}
