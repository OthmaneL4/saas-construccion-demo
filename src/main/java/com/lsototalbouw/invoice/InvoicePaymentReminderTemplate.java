package com.lsototalbouw.invoice;

import java.util.Arrays;
import java.util.Optional;

public enum InvoicePaymentReminderTemplate {
    FIRST_NOTICE(
            "FIRST_NOTICE",
            "Primer aviso",
            "Primer aviso: le recordamos amablemente que la factura sigue pendiente de pago. "
                    + "Por favor, revise la informacion de pago y confirme la fecha prevista de abono."
    ),
    SECOND_NOTICE(
            "SECOND_NOTICE",
            "Segundo aviso",
            "Segundo aviso: la factura continua pendiente tras nuestro recordatorio anterior. "
                    + "Le solicitamos regularizar el pago lo antes posible o contactar con nosotros si existe alguna incidencia."
    ),
    FINAL_NOTICE(
            "FINAL_NOTICE",
            "Aviso final",
            "Aviso final: si el pago no se regulariza en los pr d, revisaremos las medidas administrativas "
                    + "necesarias para cerrar esta deuda pendiente."
    );

    private final String code;
    private final String label;
    private final String message;

    InvoicePaymentReminderTemplate(String code, String label, String message) {
        this.code = code;
        this.label = label;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getMessage() {
        return message;
    }

    public static Optional<InvoicePaymentReminderTemplate> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(template -> template.code.equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
