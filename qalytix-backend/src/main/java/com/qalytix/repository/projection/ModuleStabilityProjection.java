package com.qalytix.repository.projection;

public interface ModuleStabilityProjection {
    String getModuleName();
    Long getTotal();
    Long getPassed();
}
