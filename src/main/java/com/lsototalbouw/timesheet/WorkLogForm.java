package com.lsototalbouw.timesheet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class WorkLogForm {

    @NotNull(message = "Selecciona un proyecto")
    private Long projectId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate workDate = LocalDate.now();

    @NotBlank(message = "El trabajador es obligatorio")
    @Size(max = 160)
    private String workerName = "Othmane";

    @NotBlank(message = "Describe el trabajo realizado")
    @Size(max = 240)
    private String description;

    @NotNull(message = "Introduce las horas")
    @DecimalMin(value = "0.25", message = "Las horas deben ser al menos 0.25")
    private BigDecimal hours = BigDecimal.ZERO;

    @NotNull(message = "Introduce la tarifa")
    @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa")
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    private boolean billable = true;

    @NotNull(message = "Selecciona un estado")
    private WorkLogStatus status = WorkLogStatus.DRAFT;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(boolean billable) {
        this.billable = billable;
    }

    public WorkLogStatus getStatus() {
        return status;
    }

    public void setStatus(WorkLogStatus status) {
        this.status = status;
    }

    public static WorkLogForm from(WorkLog workLog) {
        WorkLogForm form = new WorkLogForm();
        form.setProjectId(workLog.getProject().getId());
        form.setWorkDate(workLog.getWorkDate());
        form.setWorkerName(workLog.getWorkerName());
        form.setDescription(workLog.getDescription());
        form.setHours(workLog.getHours());
        form.setHourlyRate(workLog.getHourlyRate());
        form.setBillable(workLog.isBillable());
        form.setStatus(workLog.getStatus());
        return form;
    }
}
