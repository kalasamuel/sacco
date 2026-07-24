# Kimwanyi SACCO Management System

A full-stack Internship/SACCO management system built for Pahappa Limited,
covering member registration, savings, loans, and administration —
replacing Kimwanyi SACCO's paper ledger/passbook process described in the
project brief.

**Stack:** Java 11 · JSF 2.3 (Mojarra) · PrimeFaces 12 · Hibernate 5.6 /
JPA · MySQL 8 · Apache Maven · Apache Tomcat 9 · CDI (Weld) · BCrypt ·
Apache PDFBox

---

## 1. Prerequisites

- JDK 11+
- Apache Maven 3.8+
- MySQL 8.0+
- Apache Tomcat 9.0.x
- (Optional, for tests) nothing extra — tests run against an in-memory H2
  database, no MySQL required to run `mvn test`.

---

## 2. Database Setup

1. Create the database and schema:

   ```bash
   mysql -u root -p < db/schema.sql
   ```

   This creates the `kimwanyi_sacco` database, all tables, constraints,
   and indexes (see `db/schema.sql` — this file is also the schema
   diagram deliverable; the ER diagram was generated directly from this
   structure).

2. This also seeds one bootstrap administrator account:

   | Field | Value |
   |---|---|
   | Username | `admin` |
   | Password | `Admin@123` |

   **Change this password immediately after first login** in any real
   deployment — it exists only so the system isn't unusable on first boot.

3. Create a dedicated MySQL user for the application (don't use `root`
   in the app's connection string):

   ```sql
   CREATE USER 'sacco_app'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON kimwanyi_sacco.* TO 'sacco_app'@'localhost';
   FLUSH PRIVILEGES;
   ```

   Note: no `DELETE` privilege is actually exercised by the application
   on `transactions`, `loan_repayments`, or `audit_logs` — the service
   layer never issues a delete against those tables (Section 6) — but
   the grant above is table-scoped for MySQL simplicity. For stricter
   enforcement, revoke `DELETE` on those three tables specifically.

---

## 3. Configure the Database Connection (outside the WAR)

The database credentials are **not** stored inside the deployable WAR
(see `pom.xml` / `persistence.xml` comments for why). Instead:

1. Copy `tomcat-config/context.xml` to:
   ```
   $CATALINA_BASE/conf/Catalina/localhost/kimwanyi-sacco.xml
   ```
2. Edit it, filling in the real `username`/`password` for the
   `sacco_app` MySQL user created above.

---

## 4. Build

```bash
mvn clean package
```

Produces `target/kimwanyi-sacco.war`.

If a dependency fails to resolve, check the exact Maven Central
coordinates before assuming the version is wrong elsewhere — two version
pins were caught and corrected during this build (Mojarra `javax.faces`
and `javax.el`), so double-check any *new* dependency you add the same way.

---

## 5. Deploy

1. Copy `target/kimwanyi-sacco.war` into Tomcat's `webapps/` directory.
2. Start Tomcat.
3. Visit `http://localhost:8080/kimwanyi-sacco/`

You'll be redirected to the login page. Log in as `admin` / `Admin@123`
to reach the admin dashboard, register your first real member, and
change the admin password.

---

## 6. Running Tests

```bash
mvn test
```

Tests run against an **in-memory H2 database** (`src/test/resources/META-INF/persistence.xml`),
not MySQL — no database setup is needed to run the test suite.

### What's covered

- **`AccountTest`** — unit tests for the credit/debit invariants
  (minimum balance, insufficient funds, positive-amount validation)
  enforced directly by the `Account` entity (Section 4).
- **`LoanStateMachineTest`** — unit tests proving `Loan.transitionTo`
  only permits the legal state transitions from the workflow diagrammed
  in Section 8, and rejects everything else.
- **`ConcurrentWithdrawalTest`** — the most important test in the
  project. Two threads race to withdraw from the same account, where
  the starting balance can service only one of the two withdrawals
  without breaching the minimum balance. This directly proves the
  concurrency guarantee from Section 1: pessimistic row locking (not
  luck, not timing) is what prevents the "cashier misread the balance"
  overdraft race condition described in the brief.

  **Note:** this test runs against H2, not real MySQL/InnoDB. H2's
  `PESSIMISTIC_WRITE` locking behavior is close enough to validate the
  locking *pattern*, but isn't a substitute for also manually verifying
  this behavior against the real MySQL database before final submission
  — different engines can have subtly different lock wait semantics.

---

## 7. Default Role → Landing Page Map

| Role | Login lands on |
|---|---|
| ADMIN | `/app/admin/dashboard.xhtml` |
| LOAN_OFFICER | `/app/officer/dashboard.xhtml` |
| CASHIER | `/app/cashier/dashboard.xhtml` |
| MEMBER | `/app/member/dashboard.xhtml` |

A `MEMBER`-role login is a member logging into their own self-service
portal (view balance, apply for a loan, view/edit profile) — this role
was added in Section 8 to satisfy the brief's scope line "Member
registration, login, and profile management," alongside the
staff-mediated workflow the rest of the system assumes.

---

## 8. Architecture Summary

```
Browser
  │
  ▼
PrimeFaces views (.xhtml)  ──────────────  Section 10-14
  │
  ▼
JSF Managed Beans (@Named, CDI)  ─────────  Section 9
  │
  ▼
Service Layer (business rules, RBAC,        Section 7, 8, 9, 13, 14
  transaction boundaries, Strategy Pattern
  for interest calculation)
  │
  ▼
DAO Layer (GenericDao + entity DAOs,        Section 6
  pessimistic locking for money movement)
  │
  ▼
JPA/Hibernate Entities (guarded invariants, Section 4
  Loan state machine)
  │
  ▼
MySQL (append-only ledgers, unique          Section 2
  identity constraints, indexes)
```

Cross-cutting infrastructure: `AuthenticationFilter` (RBAC by URL path),
`CsrfFilter`, `SecurityHeadersFilter`, `PersistenceFilter`
(request-scoped EntityManager) — Sections 5 and 6.

---

## 9. Known Simplifications / Documented Assumptions

Being upfront about decisions made where the brief didn't specify
something, rather than leaving them as silent gaps:

- **Loan term**: fixed at 6 months from approval (`LoanService.LOAN_TERM_MONTHS`)
  — the brief doesn't state a term length.
- **Approval and disbursement are merged into one step** — the brief
  doesn't describe a separate disbursement workflow.
- **Interest posting is a manually triggered admin action**, not an
  automatic scheduled job — the SACCO retains deliberate control over
  when interest/overdue status changes are applied, rather than a
  background process silently mutating balances.
- **Email/system notifications** (listed as optional in the brief's
  scope) are not implemented.
- **Internal transfers** (listed as optional) are not implemented.
