package com.lsototalbouw.timesheet;

import java.math.BigDecimal;
import java.util.List;

public record WorkLogSummary(
        int totalWorkLogs,
        BigDecimal totalHours,
        BigDecimal billableHours,
        BigDecimal billableAmount,
        long pendingWorkLogs
) {

    public static WorkLogSummary from(List<WorkLog> workLogs) {
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal billableHours = BigDecimal.ZERO;
        BigDecimal billableAmount = BigDecimal.ZERO;
        long pendingWorkLogs = 0;

        for (WorkLog workLog : workLogs) {
            totalHours = totalHours.add(workLog.getHours());
            if (workLog.isBillable()) {
                billableHours = billableHours.add(workLog.getHours());
                billableAmount = billableAmount.add(workLog.getLineTotal());
            }
            if (workLog.getStatus() != WorkLogStatus.INVOICED) {
                pendingWorkLogs++;
            }
        }

        return new WorkLogSummary(workLogs.size(), totalHours, billableHours, billableAmount, pendingWorkLogs);
    }
}
