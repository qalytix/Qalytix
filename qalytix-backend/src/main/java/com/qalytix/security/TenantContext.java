package com.qalytix.security;

/**
 * Thread-local holder for the current authenticated org.
 * Set by JwtAuthFilter on every request; cleared in a finally block after the filter chain.
 * All service methods that query tenant data read orgId from here — never from request params.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> ORG_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void setOrgId(Long orgId) {
        ORG_ID.set(orgId);
    }

    public static Long getOrgId() {
        return ORG_ID.get();
    }

    public static void clear() {
        ORG_ID.remove();
    }
}
