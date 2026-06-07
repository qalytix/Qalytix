package com.qalytix.repository.projection;

public interface JobDailyStatusProjection {
    Long getJobId();
    String getJobName();
    String getDay();
    String getStatus();
}
