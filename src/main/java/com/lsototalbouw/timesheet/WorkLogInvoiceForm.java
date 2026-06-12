package com.lsototalbouw.timesheet;

import jakarta.validation.constraints.NotNull;

public class WorkLogInvoiceForm {

    @NotNull(message = "Selecciona una factura")
    private Long invoiceId;

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }
}
