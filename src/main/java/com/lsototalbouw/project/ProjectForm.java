package com.lsototalbouw.project;

import com.lsototalbouw.common.enums.ProjectStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjectForm {

    @NotNull(message = "Selecciona un cliente")
    private Long customerId;

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    @Size(max = 180)
    private String name;

    @Size(max = 240)
    private String workAddress;

    private LocalDate startDate = LocalDate.now();

    @NotNull
    private ProjectStatus status = ProjectStatus.PLANNED;

    @NotNull(message = "Introduce un presupuesto")
    @DecimalMin(value = "0.00", message = "El presupuesto no puede ser negativo")
    private BigDecimal budget = BigDecimal.ZERO;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkAddress() {
        return workAddress;
    }

    public void setWorkAddress(String workAddress) {
        this.workAddress = workAddress;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public static ProjectForm from(Project project) {
        ProjectForm form = new ProjectForm();
        form.setCustomerId(project.getCustomer().getId());
        form.setName(project.getName());
        form.setWorkAddress(project.getWorkAddress());
        form.setStartDate(project.getStartDate());
        form.setStatus(project.getStatus());
        form.setBudget(project.getBudget());
        return form;
    }
}
