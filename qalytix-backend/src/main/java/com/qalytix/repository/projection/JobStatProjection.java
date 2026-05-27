package com.qalytix.repository.projection;

public interface JobStatProjection {
    String getJobName();
    Long getTotalTests();
    String getLatestBuildStatus();
    Long getYesterdayTotal();
    Long getTodayTotal();
    Long getPassedTotal();
    Long getFailedTotal();
    Long getNoResultBuilds();
}
