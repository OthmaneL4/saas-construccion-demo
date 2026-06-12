package com.lsototalbouw.calendar;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @EntityGraph(attributePaths = "project")
    List<CalendarEvent> findByCompanyAccountIdAndActiveTrueOrderByEventDateAsc(Long companyId);

    @EntityGraph(attributePaths = "project")
    Optional<CalendarEvent> findByCompanyAccountIdAndIdAndActiveTrue(Long companyId, Long id);
}
