# LSOTOTALBOUW - Public Portfolio Demo

## Objective

Expose LSOTOTALBOUW as a safe public demo for LinkedIn, GitHub, recruiters, and technical interviewers.

This is not the same as real production. The public demo should prove the quality of the work without storing real customer data or requiring a paying client.

## Demo Positioning

Use this wording:

> LSOTOTALBOUW is a SaaS-style business management platform for construction contractors and small building companies. It demonstrates enterprise Java development with Spring Boot, Thymeleaf, Bootstrap, Spring Security, JPA/Hibernate, Flyway, audit trails, document handling, operational scripts, and production-readiness documentation.

Avoid claiming:

- It is already used by paying customers.
- It is production-certified for real sensitive data.
- It handles real client records in the public demo.

## Public Demo Credentials

Live demo: https://lsototalbouw-demo.onrender.com

```text
Email: demo@lsototalbouw.nl
Password: Demo2026!
```

The demo environment must show only fictional data.

The public demo is read-only. Visitors can navigate and inspect the application, but `POST`, `PUT`, `PATCH`, and `DELETE` actions are blocked except login/logout. This keeps the demo clean for recruiters and companies.

## Recommended Public Demo Architecture

```text
Visitor
  -> HTTPS demo URL
  -> Hosted Spring Boot application
  -> demo profile
  -> H2 demo database
  -> fictional seeded data
  -> temporary demo document directory
```

For portfolio purposes, using H2 is acceptable because the goal is to show the application flow, not operate real customer data. Real production remains MySQL-based.

## Deployment Options

### Recommended First Option: Render

Good for a fast portfolio deployment because it supports GitHub-connected web services, Docker, environment variables, health checks, managed TLS, and custom domains.

Suggested environment variables:

```text
SPRING_PROFILES_ACTIVE=demo
PORT=8080
SESSION_COOKIE_SECURE=true
DOCUMENTS_DIR=/app/uploads/demo-documents
```

### Alternative: Railway

Good for quick demos and simple GitHub-based deployment. Use the same `demo` profile.

### Alternative: VPS

More professional long term, but slower to prepare because it requires Linux server hardening, Java installation, reverse proxy, HTTPS, firewall, and process management.

## GitHub Public Repository Checklist

- No real credentials.
- No real customer data.
- No `.env` files.
- No local H2 database files.
- No uploaded private documents.
- `README.md` explains the project clearly.
- `docs/PORTFOLIO_DEMO.md` explains the demo scope.
- `.env.example` exists with placeholders only.
- CI workflow is present.
- Demo login is clearly marked as fictional.
- Public visitors cannot modify records, upload files, or change demo state.
- Production limitations are stated honestly.

## LinkedIn Post Template

```text
I built LSOTOTALBOUW, a SaaS-style business management platform for construction contractors and small building companies.

The application includes:
- Customer and project management
- Quotations and invoice workflows
- Payments, receivables, expenses, and profitability
- Materials, tools, suppliers, documents, and calendar planning
- Spring Security roles, audit logs, notifications, Flyway migrations, and production-readiness scripts

Tech stack:
Java, Spring Boot, Spring MVC, Thymeleaf, Bootstrap, Spring Security, Spring Data JPA, Hibernate, Flyway, H2/MySQL.

Demo:
[PUBLIC_DEMO_URL]

GitHub:
[PUBLIC_GITHUB_URL]

This is a public portfolio demo with fictional data. The production path is documented separately with MySQL, HTTPS, backups, restore testing, monitoring, and security hardening.
```

## Interview Talking Points

- Why MVC + Thymeleaf was chosen for fast business workflow delivery.
- How role-based access protects finance, documents, inventory, audit, and admin areas.
- How Flyway keeps database changes traceable.
- Why public demo and real production are separated.
- How document storage is validated and monitored.
- How audit logs support accountability.
- How smoke tests and Go/No-Go scripts reduce release risk.
- What would be improved next: Dockerized production, managed MySQL, monitoring, antivirus provider integration, CI/CD deployment pipeline, and deeper test coverage.
