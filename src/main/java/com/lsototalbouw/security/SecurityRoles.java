package com.lsototalbouw.security;

/**
 * Constants defining the standard authorization role names in the system.
 *
 * <p>These roles map to the Spring Security granted authorities prefixes (e.g. {@code ROLE_ADMIN}).
 */
public final class SecurityRoles {

    /** Has full tenant control, including billing modifications, user setup, and settings config. */
    public static final String OWNER = "OWNER";

    /** Equivalent administrative rights as the owner, excluding initial billing ownership. */
    public static final String ADMIN = "ADMIN";

    /** Access to financial records, including invoices, payments, and quotations. */
    public static final String FINANCE = "FINANCE";

    /** Access to site management operations, timesheets, calendar visits, and work logs. */
    public static final String OPERATIONS = "OPERATIONS";

    /** Access to inventory items, tools tracker, and material supplies lists. */
    public static final String INVENTORY = "INVENTORY";

    /** Access to upload and organize documents, logs, and blueprints. */
    public static final String DOCUMENTS = "DOCUMENTS";

    /** Access to system audit log trails. */
    public static final String AUDITOR = "AUDITOR";

    private SecurityRoles() {
    }
}
