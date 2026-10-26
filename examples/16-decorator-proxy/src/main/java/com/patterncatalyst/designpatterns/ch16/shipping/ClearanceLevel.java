package com.patterncatalyst.designpatterns.ch16.shipping;

/**
 * A caller's clearance for the access-controlled dynamic proxy. Ordinal
 * order is the comparison the proxy's invocation handler checks against;
 * declaration order below is deliberately GUEST, WAREHOUSE, ADMIN.
 */
public enum ClearanceLevel {
    GUEST,
    WAREHOUSE,
    ADMIN
}
