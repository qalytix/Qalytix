package com.qalytix.dto.response;

public record AdminPlatformStatsResponse(
        long totalOrgs,
        long activeSubscriptions,
        long totalUsers,
        long totalBuilds,
        long totalTestResults,
        long buildsLast24h,
        long testResultsLast24h
) {}
