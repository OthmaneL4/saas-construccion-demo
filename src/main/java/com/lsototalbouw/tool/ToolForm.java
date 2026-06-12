package com.lsototalbouw.tool;

import com.lsototalbouw.common.enums.ToolStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ToolForm {

    @NotBlank(message = "El nombre de la herramienta es obligatorio")
    @Size(max = 160)
    private String name;

    @Size(max = 80)
    private String serialNumber;

    @NotNull
    private ToolStatus status = ToolStatus.AVAILABLE;

    private LocalDate nextMaintenanceDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public ToolStatus getStatus() {
        return status;
    }

    public void setStatus(ToolStatus status) {
        this.status = status;
    }

    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }

    public static ToolForm from(ToolItem tool) {
        ToolForm form = new ToolForm();
        form.setName(tool.getName());
        form.setSerialNumber(tool.getSerialNumber());
        form.setStatus(tool.getStatus());
        form.setNextMaintenanceDate(tool.getNextMaintenanceDate());
        return form;
    }
}
