package com.minimarket.api.tenant;

/** Holds the tenant resolved for the current HTTP request. */
public final class TenantContext {

    private static final ThreadLocal<TenantRecord> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantRecord tenant) {
        CURRENT.set(tenant);
    }

    public static TenantRecord get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
