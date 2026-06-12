package com.lsototalbouw.timesheet;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.project.Project;
import com.lsototalbouw.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkLogService {

    private final WorkLogRepository workLogs;
    private final ProjectRepository projects;
    private final CompanyContextService companyContext;

    public WorkLogService(WorkLogRepository workLogs, ProjectRepository projects,
                          CompanyContextService companyContext) {
        this.workLogs = workLogs;
        this.projects = projects;
        this.companyContext = companyContext;
    }

    @Transactional
    public WorkLog create(WorkLogForm form) {
        Long companyId = companyContext.currentCompanyId();
        Project project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        WorkLog workLog = new WorkLog(
                project.getCompanyAccount(),
                project,
                form.getWorkDate(),
                form.getWorkerName().trim(),
                form.getDescription().trim(),
                form.getHours(),
                form.getHourlyRate(),
                form.isBillable(),
                form.getStatus()
        );
        return workLogs.save(workLog);
    }

    @Transactional(readOnly = true)
    public WorkLog getCurrentCompanyWorkLog(Long id) {
        return workLogs.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Parte de horas no encontrado"));
    }

    @Transactional
    public WorkLog update(Long id, WorkLogForm form) {
        Long companyId = companyContext.currentCompanyId();
        WorkLog workLog = workLogs.findByCompanyAccountIdAndIdAndActiveTrue(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Parte de horas no encontrado"));
        Project project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        workLog.updateFrom(
                project,
                form.getWorkDate(),
                form.getWorkerName().trim(),
                form.getDescription().trim(),
                form.getHours(),
                form.getHourlyRate(),
                form.isBillable(),
                form.getStatus()
        );
        return workLog;
    }

    @Transactional
    public void archive(Long id) {
        WorkLog workLog = getCurrentCompanyWorkLog(id);
        workLog.setActive(false);
    }
}
