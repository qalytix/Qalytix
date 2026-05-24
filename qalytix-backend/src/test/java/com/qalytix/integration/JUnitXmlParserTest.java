package com.qalytix.integration;

import com.qalytix.entity.enums.TestStatus;
import com.qalytix.integration.junit.JUnitXmlParser;
import com.qalytix.integration.junit.ParsedTestCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JUnitXmlParserTest {

    private final JUnitXmlParser parser = new JUnitXmlParser();

    private static final String SAMPLE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="com.example.ServiceTest" tests="4" failures="1" errors="1" time="1.5">
              <testcase classname="com.example.ServiceTest" name="shouldPass" time="0.3"/>
              <testcase classname="com.example.ServiceTest" name="shouldFail" time="0.2">
                <failure message="expected true but was false" type="org.junit.AssertionError">stack trace here</failure>
              </testcase>
              <testcase classname="com.example.ServiceTest" name="shouldSkip" time="0.0">
                <skipped/>
              </testcase>
              <testcase classname="com.example.ServiceTest" name="shouldError" time="0.1">
                <error message="NullPointerException" type="java.lang.NullPointerException">NPE trace</error>
              </testcase>
            </testsuite>
            """;

    @Test
    void parse_detectsAllStatuses() {
        List<ParsedTestCase> results = parser.parse(SAMPLE_XML);

        assertThat(results).hasSize(4);
        assertThat(results).anyMatch(tc -> tc.testName().equals("shouldPass") && tc.status() == TestStatus.PASSED);
        assertThat(results).anyMatch(tc -> tc.testName().equals("shouldFail") && tc.status() == TestStatus.FAILED);
        assertThat(results).anyMatch(tc -> tc.testName().equals("shouldSkip") && tc.status() == TestStatus.SKIPPED);
        assertThat(results).anyMatch(tc -> tc.testName().equals("shouldError") && tc.status() == TestStatus.ERROR);
    }

    @Test
    void parse_extractsFailureMessage() {
        List<ParsedTestCase> results = parser.parse(SAMPLE_XML);

        ParsedTestCase failed = results.stream()
                .filter(tc -> tc.testName().equals("shouldFail"))
                .findFirst().orElseThrow();

        assertThat(failed.failureMessage()).contains("expected true but was false");
    }

    @Test
    void parse_usesSuiteNameWhenClassnameBlank() {
        String xml = """
                <testsuite name="MyTestSuite">
                  <testcase classname="" name="testX" time="0.1"/>
                </testsuite>
                """;

        List<ParsedTestCase> results = parser.parse(xml);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).testSuite()).isEqualTo("MyTestSuite");
    }

    @Test
    void parse_convertsDurationCorrectly() {
        List<ParsedTestCase> results = parser.parse(SAMPLE_XML);

        ParsedTestCase passed = results.stream()
                .filter(tc -> tc.testName().equals("shouldPass"))
                .findFirst().orElseThrow();

        assertThat(passed.durationMs()).isEqualTo(300L);
    }

    @Test
    void parse_returnsEmptyListOnMalformedXml() {
        List<ParsedTestCase> results = parser.parse("not xml at all <<<");
        assertThat(results).isEmpty();
    }

    @Test
    void parse_handlesTestsuitesWrapper() {
        String xml = """
                <testsuites>
                  <testsuite name="Suite1">
                    <testcase classname="A" name="test1" time="0.1"/>
                  </testsuite>
                  <testsuite name="Suite2">
                    <testcase classname="B" name="test2" time="0.2"/>
                  </testsuite>
                </testsuites>
                """;

        List<ParsedTestCase> results = parser.parse(xml);
        assertThat(results).hasSize(2);
    }
}
