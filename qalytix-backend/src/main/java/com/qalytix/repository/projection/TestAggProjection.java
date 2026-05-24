package com.qalytix.repository.projection;

public interface TestAggProjection {
    String getTestSuite();
    String getTestName();
    Long getTotalRuns();
    Long getFailCount();
    Long getPassCount();
}
