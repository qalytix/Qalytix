package com.qalytix.integration;

import com.qalytix.entity.enums.TestStatus;
import com.qalytix.integration.junit.ParsedTestCase;
import com.qalytix.integration.testng.TestNGXmlParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestNGXmlParserTest {

    private final TestNGXmlParser parser = new TestNGXmlParser();

    // ------------------------------------------------------------
    // Realistic testng-results.xml covering all four statuses
    // ------------------------------------------------------------
    private static final String FULL_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testng-results ignored="0" total="5" failed="1" passed="3" skipped="1">
              <reporter-output/>
              <suite name="Suite" duration-ms="950">
                <test name="LoginTests" duration-ms="950">
                  <class name="com.example.LoginTest">
                    <!-- @BeforeClass — must be ignored -->
                    <test-method status="PASS" is-config="true" name="setUp" duration-ms="5"/>
                    <!-- real test methods -->
                    <test-method status="PASS" name="loginWithValidCredentials" duration-ms="310"/>
                    <test-method status="PASS" name="loginRemembersSession"      duration-ms="220"/>
                    <test-method status="FAIL" name="loginWithExpiredToken"      duration-ms="180">
                      <exception class="org.testng.AssertionError">
                        <message><![CDATA[expected [200] but found [401]]]></message>
                        <full-stacktrace><![CDATA[org.testng.AssertionError: expected [200] but found [401]
                at com.example.LoginTest.loginWithExpiredToken(LoginTest.java:55)]]></full-stacktrace>
                      </exception>
                    </test-method>
                    <test-method status="SKIP" name="loginWithSSONotImplemented" duration-ms="0"/>
                    <!-- @AfterClass — must be ignored -->
                    <test-method status="PASS" is-config="true" name="tearDown" duration-ms="3"/>
                  </class>
                </test>
              </suite>
            </testng-results>
            """;

    @Test
    void parse_returnsOnlyTestMethods_notConfigMethods() {
        List<ParsedTestCase> results = parser.parse(FULL_XML);

        // setUp and tearDown are is-config="true" — must be excluded
        assertThat(results).hasSize(4);
        assertThat(results).noneMatch(tc -> tc.testName().equals("setUp") || tc.testName().equals("tearDown"));
    }

    @Test
    void parse_detectsAllStatuses() {
        List<ParsedTestCase> results = parser.parse(FULL_XML);

        assertThat(results).anyMatch(tc ->
                tc.testName().equals("loginWithValidCredentials") && tc.status() == TestStatus.PASSED);
        assertThat(results).anyMatch(tc ->
                tc.testName().equals("loginWithExpiredToken") && tc.status() == TestStatus.FAILED);
        assertThat(results).anyMatch(tc ->
                tc.testName().equals("loginWithSSONotImplemented") && tc.status() == TestStatus.SKIPPED);
    }

    @Test
    void parse_usesSimpleClassNameAsSuite() {
        List<ParsedTestCase> results = parser.parse(FULL_XML);

        assertThat(results).allMatch(tc -> tc.testSuite().equals("LoginTest"));
    }

    @Test
    void parse_extractsFailureMessageFromCdataMessage() {
        List<ParsedTestCase> results = parser.parse(FULL_XML);

        ParsedTestCase failed = results.stream()
                .filter(tc -> tc.testName().equals("loginWithExpiredToken"))
                .findFirst().orElseThrow();

        assertThat(failed.failureMessage()).contains("expected [200] but found [401]");
    }

    @Test
    void parse_convertsDurationFromMillis() {
        List<ParsedTestCase> results = parser.parse(FULL_XML);

        ParsedTestCase passed = results.stream()
                .filter(tc -> tc.testName().equals("loginWithValidCredentials"))
                .findFirst().orElseThrow();

        assertThat(passed.durationMs()).isEqualTo(310L);
    }

    @Test
    void parse_fallsBackToStacktraceWhenNoMessageElement() {
        String xml = """
                <testng-results>
                  <suite name="S">
                    <test name="T">
                      <class name="com.example.MyTest">
                        <test-method status="FAIL" name="badTest" duration-ms="50">
                          <exception class="java.lang.RuntimeException">
                            <full-stacktrace><![CDATA[java.lang.RuntimeException: boom
                at com.example.MyTest.badTest(MyTest.java:10)]]></full-stacktrace>
                          </exception>
                        </test-method>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        List<ParsedTestCase> results = parser.parse(xml);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).failureMessage()).contains("java.lang.RuntimeException: boom");
    }

    @Test
    void parse_handlesMultipleClasses() {
        String xml = """
                <testng-results>
                  <suite name="S">
                    <test name="T">
                      <class name="com.example.ATest">
                        <test-method status="PASS" name="a1" duration-ms="10"/>
                      </class>
                      <class name="com.example.BTest">
                        <test-method status="PASS" name="b1" duration-ms="20"/>
                        <test-method status="SKIP" name="b2" duration-ms="0"/>
                      </class>
                    </test>
                  </suite>
                </testng-results>
                """;

        List<ParsedTestCase> results = parser.parse(xml);

        assertThat(results).hasSize(3);
        assertThat(results).anyMatch(tc -> tc.testSuite().equals("ATest") && tc.testName().equals("a1"));
        assertThat(results).anyMatch(tc -> tc.testSuite().equals("BTest") && tc.testName().equals("b1"));
        assertThat(results).anyMatch(tc -> tc.testSuite().equals("BTest") && tc.testName().equals("b2"));
    }

    @Test
    void parse_returnsEmptyListOnMalformedXml() {
        assertThat(parser.parse("not xml <<<")).isEmpty();
    }

    @Test
    void parse_returnsEmptyListForNonTestNGRoot() {
        String xml = "<testsuite name=\"S\"><testcase name=\"t\" classname=\"C\"/></testsuite>";
        assertThat(parser.parse(xml)).isEmpty();
    }

    // ── isTestNGXml ──────────────────────────────────────────────────────────

    @Test
    void isTestNGXml_trueForValidReport() {
        assertThat(parser.isTestNGXml(FULL_XML)).isTrue();
    }

    @Test
    void isTestNGXml_falseForJUnitXml() {
        String junit = "<testsuite name=\"S\"><testcase name=\"t\"/></testsuite>";
        assertThat(parser.isTestNGXml(junit)).isFalse();
    }

    @Test
    void isTestNGXml_falseForNull() {
        assertThat(parser.isTestNGXml(null)).isFalse();
    }

    @Test
    void isTestNGXml_falseForBlank() {
        assertThat(parser.isTestNGXml("   ")).isFalse();
    }
}
