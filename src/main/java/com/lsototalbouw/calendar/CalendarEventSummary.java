package com.lsototalbouw.calendar;

import java.time.LocalDate;
import java.util.List;

public record CalendarEventSummary(
        int totalEvents,
        long todayEvents,
        long upcomingEvents,
        long overdueEvents
) {

    public static CalendarEventSummary from(List<CalendarEvent> events, LocalDate today) {
        long todayEvents = events.stream()
                .filter(event -> event.getEventDate().isEqual(today))
                .count();
        long upcomingEvents = events.stream()
                .filter(event -> !event.getEventDate().isBefore(today))
                .count();
        long overdueEvents = events.stream()
                .filter(event -> event.getEventDate().isBefore(today))
                .count();

        return new CalendarEventSummary(events.size(), todayEvents, upcomingEvents, overdueEvents);
    }
}
