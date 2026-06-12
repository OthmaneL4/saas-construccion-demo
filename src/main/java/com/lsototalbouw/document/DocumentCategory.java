package com.lsototalbouw.document;

public enum DocumentCategory {
    CONTRACT("Contrato"),
    INVOICE("Factura"),
    RECEIPT("Recibo"),
    PROJECT_PHOTO("Foto de proyecto"),
    CUSTOMER_DOCUMENT("Documento de cliente"),
    OTHER("Otro");

    private final String label;

    DocumentCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
