package com.qalytix.integration.junit;

import com.qalytix.entity.enums.TestStatus;

public record ParsedTestCase(
        String testSuite,
        String testName,
        TestStatus status,
        long durationMs,
        String failureMessage
) {}
