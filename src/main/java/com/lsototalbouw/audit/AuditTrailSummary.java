package com.lsototalbouw.audit;

import java.util.List;

public class AuditTrailSummary {

    private final int totalEvents;
    private final int dataChangeEvents;
    private final int loginEvents;
    private final int criticalEvents;

    private AuditTrailSummary(int totalEvents, int dataChangeEvents, int loginEvents, int criticalEvents) {
        this.totalEvents = totalEvents;
        this.dataChangeEvents = dataChangeEvents;
        this.loginEvents = loginEvents;
        this.criticalEvents = criticalEvents;
    }

    public static AuditTrailSummary from(List<AuditLog> logs) {
        int dataChangeEvents = 0;
        int loginEvents = 0;
        int criticalEvents = 0;

        for (AuditLog log : logs) {
            AuditAction action = log.getAction();
            if (action == AuditAction.CREATE || action == AuditAction.UPDATE || action == AuditAction.ARCHIVE
                    || action == AuditAction.CONVERT) {
                dataChangeEvents++;
            }
            if (action == AuditAction.LOGIN_SUCCESS || action == AuditAction.LOGIN_FAILURE) {
                loginEvents++;
            }
            if (action == AuditAction.LOGIN_FAILURE || action == AuditAction.ACCOUNT_LOCKED
                    || action == AuditAction.DOCUMENT_INTEGRITY_FAILURE) {
                criticalEvents++;
            }
        }

        return new AuditTrailSummary(logs.size(), dataChangeEvents, loginEvents, criticalEvents);
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getDataChangeEvents() {
        return dataChangeEvents;
    }

    public int getLoginEvents() {
        return loginEvents;
    }

    public int getCriticalEvents() {
        return criticalEvents;
    }
}
