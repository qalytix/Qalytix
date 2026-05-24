package com.qalytix.integration.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qalytix.entity.enums.TestStatus;
import com.qalytix.integration.junit.ParsedTestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CucumberJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public boolean isCucumberJson(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (!root.isArray() || root.isEmpty()) return false;
            JsonNode first = root.get(0);
            return first.has("elements") && (first.has("uri") || first.has("keyword"));
        } catch (Exception e) {
            return false;
        }
    }

    public List<ParsedTestCase> parse(String json) {
        List<ParsedTestCase> results = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            if (!root.isArray()) return List.of();

            for (JsonNode feature : root) {
                String featureName = feature.path("name").asText("Unknown Feature");

                for (JsonNode scenario : feature.path("elements")) {
                    String keyword = scenario.path("keyword").asText("");
                    if (keyword.trim().equalsIgnoreCase("Background")) continue;

                    String scenarioName = scenario.path("name").asText("Unknown Scenario");
                    TestStatus status   = TestStatus.PASSED;
                    long durationMs     = 0;
                    String failMsg      = null;

                    for (JsonNode step : scenario.path("steps")) {
                        JsonNode result = step.path("result");
                        // Cucumber JSON durations are in nanoseconds
                        durationMs += result.path("duration").asLong(0) / 1_000_000L;
                        String stepStatus = result.path("status").asText("passed").toLowerCase();
                        switch (stepStatus) {
                            case "failed" -> {
                                status = TestStatus.FAILED;
                                String msg = result.path("error_message").asText("");
                                if (!msg.isBlank() && failMsg == null) {
                                    failMsg = msg.length() > 2000 ? msg.substring(0, 2000) : msg;
                                }
                            }
                            case "skipped", "pending", "undefined", "ambiguous" -> {
                                if (status != TestStatus.FAILED) status = TestStatus.SKIPPED;
                            }
                        }
                    }

                    results.add(new ParsedTestCase(featureName, scenarioName, status, durationMs, failMsg));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Cucumber JSON: {}", e.getMessage());
        }
        return results;
    }
}
