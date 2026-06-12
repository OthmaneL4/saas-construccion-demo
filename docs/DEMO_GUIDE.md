# LSOTOTALBOUW - Demo Guide

## Purpose

This guide defines a focused 10-15 minute product demonstration for LSOTOTALBOUW.

The demo should present the application as a professional business management platform for small construction companies and self-employed contractors, with emphasis on operational control, financial visibility, document management, and security.

## Demo Objective

By the end of the demo, the audience should understand that LSOTOTALBOUW helps a contractor:

- Manage customers, projects, quotations, invoices, payments, expenses, documents, and tools from one place.
- Move from quotation to invoice without duplicate manual work.
- Track outstanding receivables and payment reminders.
- Keep important documents linked to customers, projects, and invoices.
- Use role-based access control and audit logs for safer business operations.
- Run on a production-ready foundation with MySQL, Flyway, backups, and documented operations.

## Recommended Demo Data

Prepare realistic data before the presentation:

- Company: `LSOTOTALBOUW`
- Customer: `Horeca Plaza Noord`
- Project: `Dakrenovatie Horeca Plaza`
- Quotation: roof repair, carpentry, materials, and labor lines
- Invoice: converted from the accepted quotation
- Payment: one pending payment and one completed payment
- Document: signed quotation PDF or project photo
- Material: roof tiles or timber with low stock
- Tool: cordless drill or scaffold marked as in use or maintenance
- Calendar event: upcoming site visit
- Notification: payment follow-up or low-stock alert

Use business-like amounts and dates. Avoid fake-looking names such as "Test Customer" during the demo.

## Demo Data Preparation Checklist

Create or verify the following records before the meeting:

- One active customer with complete name, email, phone, address, and city.
- One active project linked to that customer, with budget and `In progress` status.
- One quotation linked to the customer/project with at least two line items.
- One invoice converted from that quotation.
- One invoice line showing labor or material detail.
- One pending payment linked to an invoice.
- One completed payment linked to another invoice or the same invoice if partially paid.
- One payment reminder history entry.
- One PDF document linked to the customer/project/invoice.
- One material with low stock.
- One tool in maintenance or in use.
- One supplier with contact details.
- One calendar event for an upcoming site visit.
- One unread notification.
- One audit entry for a payment action.

Clean up or archive records with names such as `QA`, `test`, `demo error`, or invalid files before presenting.

Run the demo data audit before the meeting:

```powershell
.\scripts\demo-data-audit.ps1
```

The script checks active business records for placeholder-looking values such as `test`, `QA`, `dummy`, `error`, `lorem`, and `prueba`. When `DB_URL` is not set, it audits the local H2 development database automatically. When `DB_URL` points to MySQL, it uses the configured MySQL connection values.

## Local Demo Seed

When the application runs with the `dev` profile, the development initializer creates a realistic LSOTOTALBOUW dataset automatically.

The seeded dataset includes:

- Customers, projects, quotations, invoices, payments, expenses, work logs, materials, tools, suppliers, and calendar events.
- Demo documents linked to customers, projects, and invoices.
- Unread business notifications for overdue invoices, low stock, and planned site visits.

This seed is idempotent: restarting the application should not create duplicate business records. The production profile intentionally avoids demo records.

## Demo Flow

### 1. Opening Context - 1 Minute

Explain the business problem:

Small construction companies often manage work through WhatsApp, paper notes, spreadsheets, PDF invoices, emails, and memory. LSOTOTALBOUW centralizes these workflows into one secure web platform.

Key message:

> "The goal is not only to store data, but to reduce the daily administration burden for a contractor."

### 2. Login And Dashboard - 2 Minutes

Show:

- Secure login.
- Dashboard summary.
- Invoiced amount, paid amount, expenses, and net result.
- Operational alerts.
- Recent projects.
- Open invoices.
- Quick indicators.

Key message:

> "The owner can see the health of the business immediately after login."

Avoid:

- Spending too long explaining every dashboard number.
- Opening technical configuration at this stage.

### 3. Customer And Project Control - 2 Minutes

Show:

- Customers list.
- Customer detail.
- Related projects, invoices, expenses, and documents.
- Project detail.
- Budget/status information.

Key message:

> "Every job is linked back to the customer, so information is not scattered across different tools."

### 4. Quotation To Invoice Flow - 3 Minutes

Show:

- Open an existing quotation.
- Show quotation lines and calculated total.
- Generate/download quotation PDF.
- Convert quotation to invoice.
- Open the converted invoice.
- Explain duplicate conversion protection.

Key message:

> "The contractor can turn accepted work into invoiceable revenue without retyping the same information."

### 5. Payments And Receivables - 2 Minutes

Show:

- Invoices list.
- Payment registration.
- Outstanding amount.
- Receivables page.
- Payment reminder action/PDF.

Key message:

> "The system keeps pressure on cash flow by showing what is due, overdue, and already paid."

### 6. Documents - 1 Minute

Show:

- Documents list.
- Document linked to an invoice, project, or customer.
- Preview/download.
- File availability status.

Key message:

> "Project paperwork stays connected to the business record it belongs to."

### 7. Operations, Inventory, And Calendar - 2 Minutes

Show briefly:

- Calendar event for a job visit.
- Materials with stock status.
- Tools with status.
- Suppliers.

Key message:

> "The app is not only accounting. It also supports field operations and resource tracking."

### 8. Security And Audit - 2 Minutes

Show:

- Users page.
- Role-based access concept.
- Audit page.
- Audit entry for payment, document, or login activity.

Key message:

> "Sensitive actions are controlled by role and traceable afterwards."

Avoid:

- Showing real passwords.
- Showing database credentials.
- Spending too much time in technical internals.

### 9. Production Readiness - 1 Minute

Mention:

- MySQL production profile.
- Flyway migrations.
- Secure session cookies.
- Document storage outside the database.
- Backup and restore runbook.
- Final QA checklist.

Key message:

> "This is being prepared not as a throwaway demo, but as a maintainable business application."

## Demo Closing

End with the value proposition:

> "LSOTOTALBOUW gives a small construction business a single operational cockpit: work, money, documents, and accountability in one place."

Recommended closing questions for the audience:

- Which workflow creates the most administration pain today?
- Do you need multi-company filtering from the first production release?
- Which document types are most important to manage?
- Should payment reminders be manual, automated, or both?
- What reports would help monthly decision-making?

## Demo Safety Checklist

Before presenting:

- Review `docs/GO_NO_GO_CHECKLIST.md`.
- Use demo-safe credentials.
- Do not show real database passwords.
- Do not show private customer data.
- Confirm `/dashboard` loads.
- Confirm `/quotations`, `/invoices`, `/payments`, `/documents`, and `/audit` load.
- Confirm generated PDFs open correctly.
- Confirm the browser zoom is set to 100%.
- Confirm the sidebar scrolls if needed.
- Confirm no Java stack trace is visible.
- Confirm the app is using the intended database.

## Suggested Demo Timing

| Section | Time |
| --- | ---: |
| Opening context | 1 min |
| Dashboard | 2 min |
| Customers and projects | 2 min |
| Quotation to invoice | 3 min |
| Payments and receivables | 2 min |
| Documents | 1 min |
| Operations and inventory | 2 min |
| Security and audit | 2 min |
| Production readiness | 1 min |

Total: 16 minutes. For a shorter meeting, skip the operations/inventory section and keep the demo around 12 minutes.
