package com.lsototalbouw.dashboard;

import java.time.LocalDate;

public record OperationalAlert(
        String sourceKey,
        String severity,
        String statusClass,
        String icon,
        String title,
        String message,
        String href,
        LocalDate dueDate,
        int priority
) {
}
