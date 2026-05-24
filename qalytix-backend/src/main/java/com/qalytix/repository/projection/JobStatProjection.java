package com.qalytix.repository.projection;

public interface JobStatProjection {
    String getJobName();
    Long getTotalTests();
    String getLatestBuildStatus();
    Long getYesterdayPassed();
    Long getTodayPassed();
    Long getPassedTotal();
    Long getFailedTotal();
}
