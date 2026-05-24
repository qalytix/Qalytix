package com.qalytix.integration.junit;

import com.qalytix.entity.enums.TestStatus;
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

@Component
@Slf4j
public class JUnitXmlParser {

    public List<ParsedTestCase> parse(String xml) {
        List<ParsedTestCase> results = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            // Handle both <testsuite> root and <testsuites> wrapping multiple suites
            NodeList suites = doc.getElementsByTagName("testsuite");
            for (int i = 0; i < suites.getLength(); i++) {
                Element suite = (Element) suites.item(i);
                String suiteName = suite.getAttribute("name");
                parseSuite(suite, suiteName, results);
            }
        } catch (Exception e) {
            log.warn("Failed to parse JUnit XML: {}", e.getMessage());
        }
        return results;
    }

    private void parseSuite(Element suite, String suiteName, List<ParsedTestCase> results) {
        NodeList testCases = suite.getElementsByTagName("testcase");
        for (int i = 0; i < testCases.getLength(); i++) {
            Element tc = (Element) testCases.item(i);

            String classname = tc.getAttribute("classname");
            String name      = tc.getAttribute("name");
            String effectiveSuite = !classname.isBlank() ? classname : suiteName;

            double timeSeconds = 0;
            try { timeSeconds = Double.parseDouble(tc.getAttribute("time")); } catch (NumberFormatException ignored) {}
            long durationMs = (long) (timeSeconds * 1000);

            TestStatus status;
            String failureMessage = null;

            if (tc.getElementsByTagName("failure").getLength() > 0) {
                status = TestStatus.FAILED;
                Element failure = (Element) tc.getElementsByTagName("failure").item(0);
                failureMessage = failure.getAttribute("message");
                if (failureMessage.isBlank()) failureMessage = failure.getTextContent().strip();
                if (failureMessage.length() > 2000) failureMessage = failureMessage.substring(0, 2000);
            } else if (tc.getElementsByTagName("error").getLength() > 0) {
                status = TestStatus.ERROR;
                Element error = (Element) tc.getElementsByTagName("error").item(0);
                failureMessage = error.getAttribute("message");
                if (failureMessage.isBlank()) failureMessage = error.getTextContent().strip();
                if (failureMessage.length() > 2000) failureMessage = failureMessage.substring(0, 2000);
            } else if (tc.getElementsByTagName("skipped").getLength() > 0) {
                status = TestStatus.SKIPPED;
            } else {
                status = TestStatus.PASSED;
            }

            results.add(new ParsedTestCase(effectiveSuite, name, status, durationMs, failureMessage));
        }
    }
}
