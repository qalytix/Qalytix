package com.qalytix.integration.testng;

import com.qalytix.entity.enums.TestStatus;
import com.qalytix.integration.junit.ParsedTestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the native TestNG XML report format ({@code testng-results.xml}).
 *
 * <p>TestNG report structure:</p>
 * <pre>
 * &lt;testng-results&gt;
 *   &lt;suite name="Suite"&gt;
 *     &lt;test name="LoginTests"&gt;
 *       &lt;class name="com.example.LoginTest"&gt;
 *         &lt;test-method status="PASS|FAIL|SKIP" name="testMethod" duration-ms="100"&gt;
 *           &lt;exception&gt;
 *             &lt;message&gt;...&lt;/message&gt;
 *           &lt;/exception&gt;
 *         &lt;/test-method&gt;
 *       &lt;/class&gt;
 *     &lt;/test&gt;
 *   &lt;/suite&gt;
 * &lt;/testng-results&gt;
 * </pre>
 *
 * <p>Config methods ({@code @BeforeClass}, {@code @AfterMethod}, etc.) carry
 * {@code is-config="true"} and are skipped — only actual test methods are ingested.</p>
 */
@Component
@Slf4j
public class TestNGXmlParser {

    /**
     * Returns {@code true} if the given XML string is a TestNG results file.
     * Used to skip non-TestNG artifacts without triggering a full parse.
     */
    public boolean isTestNGXml(String xml) {
        if (xml == null || xml.isBlank()) return false;
        // Fast heuristic: look for the root element name in the first 1 KB
        String head = xml.length() > 1024 ? xml.substring(0, 1024) : xml;
        return head.contains("<testng-results");
    }

    public List<ParsedTestCase> parse(String xml) {
        List<ParsedTestCase> results = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            // Root must be <testng-results>
            Element root = doc.getDocumentElement();
            if (!root.getTagName().equals("testng-results")) {
                log.debug("Not a testng-results XML — root tag is <{}>", root.getTagName());
                return List.of();
            }

            // Walk <class> elements — each one is a test class
            NodeList classes = doc.getElementsByTagName("class");
            for (int c = 0; c < classes.getLength(); c++) {
                Element clazz   = (Element) classes.item(c);
                String className = clazz.getAttribute("name");
                // Use simple class name (last segment) as the suite for readability
                String suiteName = simpleClassName(className);

                NodeList methods = clazz.getElementsByTagName("test-method");
                for (int m = 0; m < methods.getLength(); m++) {
                    Element method = (Element) methods.item(m);

                    // Skip @BeforeClass / @AfterMethod / @BeforeTest etc.
                    if ("true".equalsIgnoreCase(method.getAttribute("is-config"))) continue;

                    String methodName = method.getAttribute("name");
                    String rawStatus  = method.getAttribute("status");
                    long durationMs   = parseDuration(method.getAttribute("duration-ms"));

                    TestStatus status;
                    String failureMessage = null;

                    status = switch (rawStatus.toUpperCase()) {
                        case "PASS"  -> TestStatus.PASSED;
                        case "FAIL"  -> TestStatus.FAILED;
                        case "SKIP"  -> TestStatus.SKIPPED;
                        default      -> TestStatus.ERROR;
                    };

                    if (status == TestStatus.FAILED) {
                        failureMessage = extractExceptionMessage(method);
                    }

                    results.add(new ParsedTestCase(suiteName, methodName, status, durationMs, failureMessage));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse TestNG XML: {}", e.getMessage());
        }
        return results;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String simpleClassName(String fqn) {
        if (fqn == null || fqn.isBlank()) return "Unknown";
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }

    private long parseDuration(String raw) {
        if (raw == null || raw.isBlank()) return 0L;
        try { return Long.parseLong(raw); } catch (NumberFormatException e) { return 0L; }
    }

    private String extractExceptionMessage(Element method) {
        NodeList exceptions = method.getElementsByTagName("exception");
        if (exceptions.getLength() == 0) return null;

        Element exception = (Element) exceptions.item(0);

        // Prefer <message> child
        NodeList msgNodes = exception.getElementsByTagName("message");
        if (msgNodes.getLength() > 0) {
            String msg = msgNodes.item(0).getTextContent().strip();
            if (!msg.isBlank()) {
                return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
            }
        }

        // Fall back to first line of <full-stacktrace>
        NodeList traceNodes = exception.getElementsByTagName("full-stacktrace");
        if (traceNodes.getLength() > 0) {
            String trace = traceNodes.item(0).getTextContent().strip();
            String firstLine = trace.lines().findFirst().orElse("").strip();
            return firstLine.isBlank() ? null : (firstLine.length() > 2000 ? firstLine.substring(0, 2000) : firstLine);
        }

        return null;
    }
}
