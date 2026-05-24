package com.qalytix.repository.projection;

public interface FailureTrendProjection {
    String getDate();
    Long getTotal();
    Long getFailed();
    Long getPassed();
}
