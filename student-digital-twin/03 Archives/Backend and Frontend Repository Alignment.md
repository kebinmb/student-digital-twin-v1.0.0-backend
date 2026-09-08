# Backend and Frontend Repository Alignment Audit

**Audit Date**: September 6, 2026  
**Target Codebases**: `student-digital-twin-v1.0.0-backend` (Spring Boot 4.1.0 / Java 21) & `student-digital-twin-v1.0.0-frontend` (Angular 19 / PrimeNG 21)  
**Audit Scope**: System Synchronization, Endpoint Mapping, Architecture & State Contracts, and Cross-Vault References  

---

## 1. Executive Summary & Verification Status

A comprehensive full-stack synchronization audit was conducted across the backend and frontend repositories of the **Student Digital Twin System**. Both repositories are fully compiled, verified (`BUILD SUCCESS`), and operational across all implemented layers:

- **Backend**: Spring Boot 4.1.0+ (Jakarta EE 11, Hibernate 7, Argon2id, Virtual Threads, Spring Security, JPA).
- **Frontend**: Angular 19 (Zoneless, Signals, Control Flow `@if`/`@for`/`@let`), PrimeNG 21 (`p-drawer`, `p-table`, `p-skeleton`, `p-toast`).

---

## 2. System Synchronization: Full-Stack Endpoint & UI Mapping

| Feature Domain | Backend Controller / Service | Endpoint Path & Method | Frontend Service / Signal Store | UI Component & View State | Alignment Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Authentication** | `AuthenticationController.java`<br>`AuthService.java` | `POST /api/public/auth/login` | `AuthService.ts`<br>`accessTokenSignal` | `LoginComponent`<br>(Split-Card Layout) | **ALIGNED** (Dual-token: In-Memory JWT + HttpOnly Cookie) |
| **User Registration** | `AuthenticationController.java` | `POST /api/public/auth/register` | `AuthService.ts` | Registration Forms | **ALIGNED** (Default `ROLE_STUDENT`, Argon2id hash) |
| **Token Refresh** | `AuthenticationController.java` | `POST /api/public/auth/refresh` | `authInterceptor.ts`<br>`AuthService.ts` | Silent background refresh | **ALIGNED** (Atomic rotation & theft detection purge) |
| **Logout** | `AuthenticationController.java` | `POST /api/public/auth/logout` | `AuthService.logout()` | `DashboardLayoutComponent` | **ALIGNED** (Cookie expiration `Max-Age=0`) |
| **Password Reset** | `PasswordResetController.java`<br>`PasswordResetService.java` | `POST /api/public/auth/forgot-password`<br>`POST /api/public/auth/reset-password` | `PasswordResetService.ts` | `ForgotPasswordComponent`<br>`ResetPasswordComponent` | **ALIGNED** (30-min single-use token, JavaMailSender) |
| **Institutional Master** | `CampusController.java`<br>`DepartmentController.java`<br>`ProgramController.java` | `GET/POST /api/v1/campuses`<br>`GET/POST /api/v1/departments`<br>`GET/POST /api/v1/programs` | `InstitutionService.ts` | `CampusManagerComponent`<br>`DepartmentManagerComponent`<br>`ProgramManagerComponent` | **ALIGNED** (Master-Detail `p-drawer` & `p-skeleton` table) |
| **Academic Term Control** | `AcademicYearController.java`<br>`TermController.java` | `GET/POST /api/v1/academic-years`<br>`GET/POST /api/v1/terms` | `InstitutionService.ts` | `AcademicYearManagerComponent`<br>`TermManagerComponent` | **ALIGNED** (Term status toggle, `p-drawer` detail) |
| **Grading & Transmutation** | `GradingScaleController.java` | `GET/POST /api/v1/grading-scales` | `GradingScaleService.ts` | `GradingScaleManagerComponent` | **ALIGNED** (Live transmutation calculator simulator) |
| **Outcome Matrix** | `CiloPiloMappingController.java` | `GET/POST /api/v1/cilo-pilo-mappings` | `CiloPiloMappingService.ts` | `CiloPiloMatrixComponent` | **ALIGNED** (2D I/E/D alignment matrix) |
| **Financial & Fees** | `FinancialController.java` | `GET/POST /api/v1/financial/*` | `FinancialService.ts` | `FinancialFoundationsComponent` | **ALIGNED** (Tuition, FHE billable, UniFAST scholarships) |
| **Class Section Builder** | `SchedulingController.java`<br>`SchedulingService.java` | `GET/POST /api/v1/scheduling/sections` | `SchedulingStore.ts` | `SectionBuilderComponent` | **ALIGNED** (CHED CMO 25 duration check, `p-drawer` detail) |
| **Faculty Workload** | `SchedulingController.java` | `GET /api/v1/scheduling/workload`<br>`POST /api/v1/scheduling/overload-approval` | `SchedulingStore.ts` | `SectionBuilderComponent`<br>(Faculty Load Modal) | **ALIGNED** (Dynamic load cap: 18h/21h & Admin override) |
| **Timetable Matrix** | `SchedulingController.java` | `GET /api/v1/scheduling/sections` | `SchedulingStore.ts` | `TimetableGridComponent` | **ALIGNED** (Weekly slot cards & room filter) |
| **Curriculum Designer** | `CurriculumController.java`<br>`CurriculumDesignerService.java` | `GET/POST /api/v1/curricula`<br>`POST /api/v1/curricula/{id}/clone`<br>`POST /api/v1/curricula/{id}/status` | `CurriculumDesignerStore.ts` | `CurriculumDesignerComponent`<br>`CoursePaletteDrawerComponent`<br>`PrerequisiteDagComponent`<br>`ObeMatrixComponent` | **ALIGNED** (Drag-and-Drop board, DAG visualizer, Bento cards) |
| **Student Advising** | `EnrollmentController.java` | `GET /api/v1/enrollment/advising` | `EnrollmentStore.ts` | `StudentAdvisingComponent` | **ALIGNED** (Prerequisite check & `p-drawer` enlistment) |

---

## 3. Architectural Overview & Integration Touchpoints

### 3.1 Security & Dual-Token Architecture
- **In-Memory JWT Access Token**: Short-lived (15 minutes), signed with RSA256 (`TokenService`). Stored strictly in Angular Signals (`accessTokenSignal`), completely isolated from XSS vectors (`localStorage`/`sessionStorage`).
- **Partitioned HttpOnly Refresh Cookie**: Long-lived (7 days), stored in `refresh_tokens` MySQL table with atomic conditional rotation (`revokeIfActive`). Delivered via `Set-Cookie: REFRESH_TOKEN=...; Path=/api/public/auth; HttpOnly; Secure; SameSite=None; Partitioned`.
- **Theft Protection & Token Purge**: Replay of a revoked refresh token immediately purges all active user sessions (`deleteAllByUserId`).
- **Spring Security & Virtual Threads**: Spring Security filter chains running on Java 21 Virtual Threads with Micrometer `ContextSnapshotFactory` for context propagation.

### 3.2 UI Design System & Component Conventions
- **Institutional Palette**: CHMSU Forest Green (`#116834`), Slate/Navy neutrals (`#0f172a`, `#1e293b`), crisp 1px borders (`#e2e8f0`).
- **Master-Detail Drawer Pattern**: Replaced traditional modal dialogs with right-anchored `<p-drawer position="right">` panels (`.institutional-drawer`) triggered by table row selection clicks.
- **Async Feedback Standardization**: All datatables utilize `<ng-template pTemplate="loadingbody">` with `<p-skeleton>` rows. Global feedback uses `<p-toast position="top-right">`.
- **CHED CMO No. 25 Compliance**: Real-time duration calculation (lecture units x 60 mins, lab units x 180 mins) and dynamic faculty workload caps (18 hrs for >2 preps, 21 hrs for <=2 preps, maximum cap 24 contact hours).

---

## 4. Bi-Directional Vault Relationships & Cross-Links

This note connects the project modules, specifications, and reference guidelines across the Obsidian vault:

### 4.1 Project Deliverables & Milestones (`00 Project`)
- [[00 Module Implementation]]: High-level sitemap and 8-phase implementation roadmap.
- [[01 Layer 1 System Master Data & RBAC]]: Master tables, RBAC roles (`ADMIN`, `DEAN`, `CHAIRPERSON`, `FACULTY`, `REGISTRAR`, `STUDENT`), and financial fee matrices.
- [[02 Layer 2 Curriculum & Outcome-Based Education (OBE)]]: Domain entity architecture, Flyway schema blueprints, prerequisite DAG rule engine, and CILO-PILO mapping.

### 4.2 Technical Standards & Security Specs (`02 Resources`)
- [[00 Spring  Security - Virtual Threads Context Propagation]]: SecurityContextHolder snapshot propagation across Java 21 Virtual Threads.
- [[01 Outbound SSRF Mitigation in RestClient]]: RestClient outbound HTTP request hardening.
- [[02 OAuth 2.1 Strict Compliance & JWT Validation]]: OAuth 2.1 JWT validation and audience checks.
- [[03 gRPC 1.1 Security Interceptors]]: gRPC authentication interceptors and JSpecify null-safety.
- [[04 Jackson 3 Polymorphic Deserialization Hardening]]: Jackson 3 polymorphic type validation.
- [[05 Actuator Port Segregation & Filter Chains]]: Actuator port isolation (port 8081).
- [[06 Modern Security Headers & CHIPS Cookies]]: Partitioned CHIPS cookies and CSP headers.
- [[07 Cors Configuration]]: Cross-Origin Resource Sharing rules for Angular frontend (`:4200`).
- [[00 Curriculum Mapping]]: OBE outcome alignment guidelines (CILO/PILO/IILO).
- [[01 Scheduling & Subject Loading]]: Timetable scheduling, room allocation, and faculty workload caps.
- [[02 Enrollment & Registration]]: Advising and section enlistment workflows.
- [[02 Curriculum & Enrollment Management]]: Master specification for curriculum and enrolment.
- [[00 Admissions & Student Profiling]]: Student profile intake and UniFAST equity target indicators.
- [[01 Academic Records & Credentialing]]: Permanent student transcripts and grade sealing.
- [[02 Graduation & Special Orders (SO)]]: Special Order (SO) audit packets for CHED graduation compliance.
- [[00 Faculty Profiling]]: Faculty credentials, PRC licenses, and academic ranks.
- [[01 Teaching Workload]]: CHED contact hour caps and overload approval authorization.
- [[02 Grade Encoding & Verification]]: Electronic grade encoding, verification, and locking.
- [[00 Tuition & Miscellaneous Assessment]]: Assessment logic for CHED-sanctioned fees.
- [[01 Billing, Cashiering & Ledgering]]: Student ledgering and payment term templates.
- [[02 Scholarship & UniFAST Billing]]: FHE billing manifests and UniFAST TES subsidies.
- [[00 Statistical Data Generation]]: CHED HEMIS Form E-1 through E-5 statistical reports.
- [[01 Interoperability]]: Government portal data exchange formats.
- [[00 Student & Faculty Self-Service]]: Student Digital Twin self-service portals.
- [[00 Real-Time Telemetry Processing]]: Attendance geofencing and continuous assessment telemetry.
- [[01 Longitudinal Academic Graph Modeling]]: Student competency twin graph modeling.

### 4.3 Testing Guidelines (`04 Manual Testing`)
- [[01 Institutional Setup & Curriculum Designer Manual Testing Guide (CHED CMO 25 s. 2015)]]: End-to-end manual verification guide for CHED CMO 25 compliance.

### 4.4 Historical Phase Audits (`00 AI Audit` & `03 Archives`)
- [[00 Audit and Testing using AI]]: Backend testing & database isolation audit.
- [[01 Login and Authentication Backend and Frontend Implementation]]: Full-stack login and authentication technical audit.
- [[03 Phase 1 Foundation Layer Implementation Audit & Phase 2 Readiness Sign-Off]]: Phase 1 completion and readiness sign-off.
- [[09 Phase 2 Completion & Phase 3 Transition Readiness Report]]: Phase 2 completion and Phase 3 transition readiness report.
- [[00 Login and Authentication Backend + Frontend]]: Archived authentication technical summary.

---

## 5. Audit Findings & Vault Maintenance Recommendations

1. **Space-Prefixed Duplicate File Detection**:
   - **Path**: `02 Resources/ 02 Curriculum & Enrollment Management.md` (contains a leading space in the filename).
   - **Recommendation**: Candidate for archival/deletion or renaming to match `02 Resources/02 Curriculum & Enrollment Management.md`. (Flagged for manual user action).
2. **Unpopulated Directory (`01 Areas/`)**:
   - **Path**: `01 Areas/` is currently empty.
   - **Recommendation**: Populate `01 Areas/` with long-term domain area notes (e.g., `Auth & Security`, `UI/UX Standards`, `CHED Compliance`) as the project transitions to Phase 3.
