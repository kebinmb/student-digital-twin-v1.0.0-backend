# Role-Based Access Control (RBAC) & Endpoint Security Specification

**Document Version:** 1.1.0  
**Target Platform:** Student Digital Twin (`SDT-v1.0.0`)  
**Backend Framework:** Java 21 / Spring Boot 4.1.1 / Spring Security 6.x  
**Frontend Framework:** Angular 19+ / PrimeNG  
**Primary Configuration References:**
- [`WebSecurityConfig.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/config/WebSecurityConfig.java)
- [`ActuatorSecurityConfig.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/config/ActuatorSecurityConfig.java)
- [`JwtRoleConverter.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/config/JwtRoleConverter.java)
- [`Roles.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/entities/authentication/Roles.java)
- [`DataScopingService.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/service/security/DataScopingService.java)

---

## 1. Executive Architecture Overview

The Student Digital Twin platform enforces a multi-layered, defense-in-depth authorization model combining stateless OAuth 2.1 JWT tokens, URL-level security filter chains, declarative method security (`@EnableMethodSecurity`), and programmatic departmental data scoping.

```
+---------------------------------------------------------------------------------------------------+
|                                      Client Request (Angular 19+)                                 |
+---------------------------------------------------------------------------------------------------+
                                                  |
                                                  v
+---------------------------------------------------------------------------------------------------+
| 1. ActuatorSecurityConfig (@Order(1))                                                             |
|    - Matcher: EndpointRequest.toAnyEndpoint()                                                     |
|    - /actuator/health, /actuator/info          -> permitAll()                                     |
|    - /actuator/prometheus, /actuator/metrics   -> hasRole("MONITORING")                           |
|    - Other endpoints                           -> hasRole("INFRA_ADMIN")                          |
+---------------------------------------------------------------------------------------------------+
                                                  | (Non-actuator traffic)
                                                  v
+---------------------------------------------------------------------------------------------------+
| 2. WebSecurityConfig (@Order(2))                                                                  |
|    - Matcher: /api/**, /ws/**                                                                     |
|    - SessionCreationPolicy: STATELESS                                                             |
|    - /api/public/**                            -> permitAll()                                     |
|    - /api/admin/**                             -> hasRole("ADMIN")                                |
|    - anyRequest()                              -> authenticated()                                 |
+---------------------------------------------------------------------------------------------------+
                                                  | (Token Validation & Role Extraction)
                                                  v
+---------------------------------------------------------------------------------------------------+
| 3. JwtRoleConverter                                                                               |
|    - Extracts claims from `roles` or `realm_access.roles`                                         |
|    - Normalizes strings to Spring Security GrantedAuthority: `ROLE_<NAME>`                        |
+---------------------------------------------------------------------------------------------------+
                                                  | (Method Execution & Ownership)
                                                  v
+---------------------------------------------------------------------------------------------------+
| 4. Method-Level Security (@PreAuthorize / Custom Evaluator Beans)                                |
|    - Static Role Check: hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', ...)                           |
|    - Fine-Grained Ownership Evaluators:                                                           |
|        * @sectionSecurity.isInstructor(sectionId, auth)                                           |
|        * @sectionSecurity.isInstructorForItem(itemId, auth)                                       |
|        * @enrollmentSecurity.canAccessStudentAdvising(auth, studentId)                             |
|        * @enrollmentSecurity.canAccessStudentEnrollment(auth, studentId)                           |
|        * @facultySecurity.isFacultySelf(userId, auth)                                             |
+---------------------------------------------------------------------------------------------------+
                                                  | (Domain-Level Scoping)
                                                  v
+---------------------------------------------------------------------------------------------------+
| 5. Programmatic Departmental Data Scoping (DataScopingService)                                    |
|    - DEAN: Filtered to College and all child departments (findProgramsByCollege)                  |
|    - CHAIRPERSON: Filtered to assigned Program (chairpersonUserId = currentUserId)                |
|    - FACULTY: Filtered to assigned teaching load / timetable slots                                |
|    - Clearance & Enrollment Gates: Checked on self-enlistment (Financial & Departmental)          |
+---------------------------------------------------------------------------------------------------+
```

### 1.1 Custom Security Evaluator Beans & Data Scoping Architecture
The backend implements domain-aware Spring Security expression beans and data scoping services to defend against **Insecure Direct Object References (IDOR)** and enforce institutional boundaries:
1. **[`SectionSecurity.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/service/scheduling/SectionSecurity.java) (`@sectionSecurity`)**:
   - `isInstructor(sectionId, authentication)`: Verifies if the authenticated user is either the assigned `primaryInstructor` or listed in any schedule slot for the section.
   - `isInstructorForItem(itemId, authentication)`: Resolves the parent section from the `ClassRecordItem` ID and executes the instructor check.
2. **[`EnrollmentSecurity.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/service/enrollment/EnrollmentSecurity.java) (`@enrollmentSecurity`)**:
   - `canAccessStudentAdvising(authentication, studentId)`: Authorizes institutional staff (`ADMIN`, `REGISTRAR`, `DEAN`, `CHAIRPERSON`, `FACULTY`) to view any student's advising, or restricts `STUDENT` callers strictly to their own student profile ID.
   - `canAccessStudentEnrollment(authentication, studentId)`: Restricts write/enlistment actions to enrollment staff (`ADMIN`, `REGISTRAR`, `DEAN`, `CHAIRPERSON`) or the student owner.
3. **[`FacultySecurity.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/service/faculty/FacultySecurity.java) (`@facultySecurity`)**:
   - `isFacultySelf(userId, authentication)`: Confirms whether the authenticated caller's resolved user ID matches the target faculty user ID.
4. **[`DataScopingService.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/service/security/DataScopingService.java)**:
   - `getScopedProgramIdsForUser(User user)`: 
     - `ADMIN`, `REGISTRAR`: Unrestricted scope (returns empty collection indicating all institutional programs).
     - `DEAN`: Resolves Dean's college department and all child departments (`departmentRepository.findByParentDepartmentId(...)`), returning all program IDs under the college.
     - `CHAIRPERSON`: Resolves program where `chairpersonUserId = user.getId()`.
     - Non-academic staff: Empty scoped list.
   - `isOnlyFaculty(User user)`: Distinguishes standalone faculty instructors from those with administrative roles (`ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`).
5. **Clearance & Enrollment Gates**:
   - `StudentProfile.financialClearance` and `departmentalClearance` (`CLEARED`, `PENDING`, `BLOCKED`).
   - Self-enlistment and enrollment confirmation throw `IllegalStateException` unless `financialClearance == CLEARED` and `departmentalClearance == CLEARED`.

---

## 2. Institutional Role Taxonomy & Hierarchy

The application defines 9 institutional roles in [`Roles.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/entities/authentication/Roles.java), mapped to granted authorities formatted as `ROLE_<ROLE_NAME>`:

| Role Name | Authority String | Institutional Scope & Responsibilities | Intake Module |
| :--- | :--- | :--- | :--- |
| **`SUPER_ADMIN`** | `ROLE_SUPER_ADMIN` | Reserved for root-level infrastructure maintenance and cross-tenant configuration. Super-set privileges. | Root CLI / DB |
| **`ADMIN`** | `ROLE_ADMIN` | Institutional system administrators. Full read/write access to institutional registry, curricula, schedules, financial settings, and user provisioning. System-wide scope. | User Accounts (`/api/v1/users`), Faculty Accounts (`/api/v1/faculty`) |
| **`DEAN`** | `ROLE_DEAN` | College executive officer. Manages college departments, degree programs, curriculum design, faculty workload limits, overload approvals, and grade verifications. Scoped to assigned College and child departments. | Faculty Roster Review |
| **`CHAIRPERSON`** | `ROLE_CHAIRPERSON` | Academic department head. Designs curricula, manages CILO-PILO mappings, builds class sections, assigns instructors, and verifies submitted grades. Scoped to assigned Program. | Program Curriculum / Sections |
| **`REGISTRAR`** | `ROLE_REGISTRAR` | Office of the University Registrar. Enforces academic calendar, manages academic years/terms, opens/locks enrollment & grading windows, executes student enlistment/crediting, provisions faculty accounts, and seals official final grades. Institutional academic scope. | Faculty Accounts (`/api/v1/faculty`), User Accounts (`/api/v1/users`) |
| **`FACULTY`** | `ROLE_FACULTY` | Teaching staff. Manages assigned class records, assessment items, raw score matrices, submits grade sheets, and reviews curriculum matrices and personal workload. Scoped to assigned teaching load. | Gradebook / Assigned Load |
| **`STUDENT`** | `ROLE_STUDENT` | Enrolled or applicant student. Self-service access to own curriculum matrix, class schedules, own enrollment eligibility, section enlistment, and personal grades. Self-enrollment gated by clearance and prerequisite validation. | Self-Service Admissions / Portal |
| **`CASHIER`** | `ROLE_CASHIER` | Student accounts and financial cashiering. Authorized to evaluate tuition and update student financial clearance status (`PATCH /api/v1/students/{id}/clearance`). | Billing & Clearance |
| **`GUIDANCE`** | `ROLE_GUIDANCE` | Guidance counseling and student welfare. Intended for behavioral analytics and student advisory twins. | Student Progress |
| **`GUEST`** | *Anonymous / None* | Unauthenticated user. Limited exclusively to public authentication, password recovery, and login endpoints. | Public Portal |

### 2.1 Role Privilege Matrix

```
[SUPER_ADMIN] > [ADMIN]
                   |
     +-------------+-------------+
     |                           |
   [DEAN]                   [REGISTRAR]
     |                           |
[CHAIRPERSON]                    |
     |                           |
 [FACULTY]                       |
     |                           |
     +-------------+-------------+
                   |
               [STUDENT]
                   |
                [GUEST]
```

---

## 3. Comprehensive Controller RBAC Mapping Table

Below is the complete static mapping of all **21 `@RestController` classes** and **82 distinct REST endpoints** across the entire backend codebase.

### 3.1 Authentication & Security (`/api/public/auth`)
*Controller:* [`AuthenticationController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/authentication/AuthenticationController.java) & [`PasswordResetController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/authentication/PasswordResetController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `AuthenticationController` | `POST` | `/api/public/auth/register` | `permitAll()` (Public/Guest) | `WebSecurityConfig` matcher `/api/public/**` | Sign-up / Registration modal or page. |
| `AuthenticationController` | `POST` | `/api/public/auth/login` | `permitAll()` (Public/Guest) | `WebSecurityConfig` matcher `/api/public/**` | Login form component; returns Access JWT + sets Refresh Cookie. |
| `AuthenticationController` | `POST` | `/api/public/auth/refresh` | `permitAll()` (Public/Guest) | `WebSecurityConfig` matcher `/api/public/**` | Silent HTTP Interceptor token rotation on 401/init. |
| `AuthenticationController` | `POST` | `/api/public/auth/logout` | `permitAll()` (Public/Guest) | `WebSecurityConfig` matcher `/api/public/**` | Profile dropdown "Sign Out" button; clears auth cookies. |
| `PasswordResetController` | `POST` | `/api/public/auth/forgot-password` | `permitAll()` (Public/Guest) | `WebSecurityConfig` matcher `/api/public/**` | "Forgot Password" dialog; initiates email dispatch. |
| `PasswordResetController` | `POST` | `/api/public/auth/reset-password` | `permitAll()` (Public/Guest) | `WebSecurityConfig` matcher `/api/public/**` | Password reset view linked via token query parameter. |

---

### 3.2 User Account Administration & Provisioning (`/api/v1/users`)
*Controller:* [`UserController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/authentication/UserController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `UserController` | `POST` | `/api/v1/users` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "New User" dialog in User Management view (`/dashboard/users`). Supports attaching College and Program. |
| `UserController` | `GET` | `/api/v1/users` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | Master user directory table with role, status, and academic scope (College/Program). |
| `UserController` | `GET` | `/api/v1/users/{id}` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | User detail inspector dialog including academic scope metadata. |
| `UserController` | `PUT` | `/api/v1/users/{id}` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Edit User" dialog (role assignment, status toggle, email, College/Program affiliation). |
| `UserController` | `DELETE`| `/api/v1/users/{id}` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | Deactivate/delete user danger action button (strictly restricted to root `ADMIN`). |
| `UserController` | `GET` | `/api/v1/users/roles` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | Role selection dropdown options for user creation/editing. |

---

### 3.3 Institutional Master Data & Governance
*Controllers:* [`CampusController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/CampusController.java), [`DepartmentController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/DepartmentController.java), [`ProgramController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/ProgramController.java), [`AcademicYearController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/AcademicYearController.java), [`TermController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/TermController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `CampusController` | `POST` | `/api/v1/campuses` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | "Add Campus" modal button in Organizational Hierarchy view. |
| `CampusController` | `GET` | `/api/v1/campuses` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Campus selectors, dropdowns, and institutional directory tables. |
| `CampusController` | `GET` | `/api/v1/campuses/active` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Active campus filtering options in scheduling and admissions. |
| `CampusController` | `GET` | `/api/v1/campuses/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Campus detail header and info badges. |
| `CampusController` | `PUT` | `/api/v1/campuses/{id}` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | "Edit Campus" action in institutional settings. |
| `CampusController` | `PATCH`| `/api/v1/campuses/{id}/status` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | Campus status toggle switch (Active/Inactive). |
| `CampusController` | `DELETE`| `/api/v1/campuses/{id}` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | "Delete Campus" danger button (disabled if child depts exist). |
| `DepartmentController` | `POST` | `/api/v1/departments` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Create Department" dialog in Organizational Hierarchy view. |
| `DepartmentController` | `GET` | `/api/v1/departments` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Department data-table, faculty affiliation pickers. |
| `DepartmentController` | `GET` | `/api/v1/departments/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Department detail card view. |
| `DepartmentController` | `GET` | `/api/v1/departments/campus/{campusId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Cascading department filter based on selected campus. |
| `DepartmentController` | `PUT` | `/api/v1/departments/{id}` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Edit Department" action button. |
| `DepartmentController` | `DELETE`| `/api/v1/departments/{id}` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | "Delete Department" button (guarded by admin role). |
| `ProgramController` | `GET` | `/api/v1/programs` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Degree program listings, student curriculum lookup dropdowns. |
| `ProgramController` | `GET` | `/api/v1/programs/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Program overview header and CMO specifications panel. |
| `ProgramController` | `GET` | `/api/v1/programs/department/{departmentId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Cascading program selector within department view. |
| `ProgramController` | `POST` | `/api/v1/programs` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "New Degree Program" wizard. |
| `ProgramController` | `PUT` | `/api/v1/programs/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Update Program" form (units required, CMO reference). |
| `ProgramController` | `DELETE`| `/api/v1/programs/{id}` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Delete Program" action button (Chairperson excluded). |
| `ProgramController` | `GET` | `/api/v1/programs/{id}/outcomes` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Program Intended Learning Outcomes (PILO) display tab. |
| `ProgramController` | `POST` | `/api/v1/programs/{id}/outcomes` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Add PILO" outcome modal. |
| `ProgramController` | `DELETE`| `/api/v1/programs/outcomes/{outcomeId}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Remove PILO" button. |
| `ProgramController` | `DELETE`| `/api/v1/programs/{programId}/outcomes/{outcomeId}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | Nested outcome deletion action in curriculum designer. |
| `AcademicYearController` | `POST` | `/api/v1/academic-years` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Create Academic Year" form in Academic Periods view. |
| `AcademicYearController` | `GET` | `/api/v1/academic-years` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Academic year global navigation picker and historical tables. |
| `AcademicYearController` | `GET` | `/api/v1/academic-years/current` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Global header banner indicating current operational year. |
| `AcademicYearController` | `GET` | `/api/v1/academic-years/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Academic year detail inspector. |
| `AcademicYearController` | `PUT` | `/api/v1/academic-years/{id}` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Edit Academic Year" dialog. |
| `AcademicYearController` | `PUT` | `/api/v1/academic-years/{id}/set-current` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Make Current Operational Year" action button. |
| `AcademicYearController` | `DELETE`| `/api/v1/academic-years/{id}` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | "Delete Academic Year" danger button (Admin only). |
| `TermController` | `GET` | `/api/v1/terms` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Semester / Term selector across all operational modules. |
| `TermController` | `GET` | `/api/v1/terms/academic-year/{academicYearId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Filtered term list for a specific academic year. |
| `TermController` | `GET` | `/api/v1/terms/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Term schedule metadata and status indicators. |
| `TermController` | `POST` | `/api/v1/terms` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "Add Term / Semester" button in Academic Periods view. |
| `TermController` | `PUT` | `/api/v1/terms/{id}/schedule` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | Datepicker for Term Start and End dates. |
| `TermController` | `PUT` | `/api/v1/terms/{id}/activate` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Activate Term" switch in Academic Periods view. |
| `TermController` | `PUT` | `/api/v1/terms/{id}/enrollment-window` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Open/Close Enrollment Window" master control. |
| `TermController` | `PUT` | `/api/v1/terms/{id}/grading-window` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Open/Lock Grade Submission Window" master control. |
| `TermController` | `PUT` | `/api/v1/terms/{id}/add-drop-window` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Toggle Add/Drop Period" master control. |
| `TermController` | `DELETE`| `/api/v1/terms/{id}` | `ADMIN` | `@PreAuthorize("hasAnyRole('ADMIN')")` | "Delete Term" button (Admin only). |

---

### 3.4 Outcome-Based Education (OBE) & Curriculum Management
*Controllers:* [`CurriculumController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/CurriculumController.java), [`CourseController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/CourseController.java), [`CourseOutcomeController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/CourseOutcomeController.java), [`CiloPiloMappingController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/CiloPiloMappingController.java), [`CoursePrerequisiteController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/CoursePrerequisiteController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `CurriculumController` | `GET` | `/api/v1/curricula/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Curriculum viewer header, metadata, and status badge. |
| `CurriculumController` | `PUT` | `/api/v1/curricula/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Edit Curriculum" dialog (version, effective term). |
| `CurriculumController` | `DELETE`| `/api/v1/curricula/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Delete Draft Curriculum" action. |
| `CurriculumController` | `GET` | `/api/v1/curricula/{id}/courses` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Tabular course list within a specific curriculum. |
| `CurriculumController` | `POST` | `/api/v1/curricula` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "New Curriculum" button in Curriculum Designer. |
| `CurriculumController` | `POST` | `/api/v1/curricula/{id}/courses` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | Drag-and-drop course assignment into Year/Semester buckets. |
| `CurriculumController` | `DELETE`| `/api/v1/curricula/{id}/courses/{curriculumCourseId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | Trash icon on course nodes in Curriculum Designer. |
| `CurriculumController` | `POST` | `/api/v1/curricula/{id}/clone` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | "Clone as New Revision" button in Curriculum Designer. |
| `CurriculumController` | `GET` | `/api/v1/curricula/{id}/designer` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY` | `@PreAuthorize("hasAnyRole(...)")` | Main interactive CDK drag-and-drop Curriculum Designer canvas. |
| `CurriculumController` | `PUT` | `/api/v1/curricula/{id}/courses/position` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | Single node repositioning in curriculum grid. |
| `CurriculumController` | `POST` | `/api/v1/curricula/{id}/prerequisites` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | Prerequisite drawer connection tool (connect source -> target). |
| `CurriculumController` | `POST` | `/api/v1/curricula/{id}/validate` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | "Validate Structure" button; triggers cycle detection & unit audits. |
| `CurriculumController` | `POST` | `/api/v1/curricula/{id}/transition-state` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | "Submit for Approval", "Approve", or "Deprecate" status actions. |
| `CurriculumController` | `GET` | `/api/v1/curricula/{id}/available-courses` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | Available Courses side-drawer (unassigned subjects palette). |
| `CurriculumController` | `PUT` | `/api/v1/curricula/{id}/courses/batch-positions` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | Batch reorder event emitted after drag-and-drop drop-list reordering. |
| `CurriculumController` | `DELETE`| `/api/v1/curricula/{id}/prerequisites/{prerequisiteId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | "Unlink Prerequisite" button on course node relations. |
| `CurriculumController` | `GET` | `/api/v1/curricula/program/{programId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY` | `@PreAuthorize("hasAnyRole(...)")` | Curriculum version history list for a program. |
| `CurriculumController` | `GET` | `/api/v1/curricula`, `/lookup` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Curriculum lookup dropdown in Student Advising and Enrollment. |
| `CourseController` | `POST` | `/api/v1/courses` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Create Subject" modal in Course Catalog Manager. |
| `CourseController` | `GET` | `/api/v1/courses` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Master course catalog table, course auto-completes. |
| `CourseController` | `GET` | `/api/v1/courses/search` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Debounced course search input in curriculum & scheduling. |
| `CourseController` | `GET` | `/api/v1/courses/active` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Active course filtering in section timetable scheduler. |
| `CourseController` | `GET` | `/api/v1/courses/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Course syllabus view and credit units specification. |
| `CourseController` | `PUT` | `/api/v1/courses/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Edit Course" dialog in Course Catalog Manager. |
| `CourseController` | `PATCH`| `/api/v1/courses/{id}/status` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | Course active/inactive toggle switch. |
| `CourseController` | `DELETE`| `/api/v1/courses/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Delete Course" button (disabled if referenced in curricula). |
| `CourseOutcomeController` | `POST` | `/api/v1/courses/{courseId}/outcomes` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Add CILO" button in Course Syllabus / Outcome tab. |
| `CourseOutcomeController` | `GET` | `/api/v1/courses/{courseId}/outcomes` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Course Intended Learning Outcomes (CILO) table. |
| `CourseOutcomeController` | `GET` | `/api/v1/courses/{courseId}/outcomes/{outcomeId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | CILO detail card. |
| `CourseOutcomeController` | `PUT` | `/api/v1/courses/{courseId}/outcomes/{outcomeId}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Edit CILO" modal dialog. |
| `CourseOutcomeController` | `DELETE`| `/api/v1/courses/{courseId}/outcomes/{outcomeId}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Delete CILO" button. |
| `CiloPiloMappingController` | `POST` | `/api/v1/cilo-pilo-mappings` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | Interactive checkbox / cell toggle in CILO-PILO Alignment Matrix. |
| `CiloPiloMappingController` | `GET` | `/api/v1/cilo-pilo-mappings` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Master CILO-PILO heatmap matrix grid view. |
| `CiloPiloMappingController` | `GET` | `/api/v1/cilo-pilo-mappings/course/{courseId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Alignment summary within Course Syllabus editor. |
| `CiloPiloMappingController` | `GET` | `/api/v1/cilo-pilo-mappings/program/{programId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Degree program OBE accreditation compliance report. |
| `CiloPiloMappingController` | `GET` | `/api/v1/cilo-pilo-mappings/course-outcome/{ciloId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Single CILO alignment inspector. |
| `CiloPiloMappingController` | `GET` | `/api/v1/cilo-pilo-mappings/program-outcome/{piloId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Single PILO coverage breakdown inspector. |
| `CiloPiloMappingController` | `DELETE`| `/api/v1/cilo-pilo-mappings/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole(...)")` | "Remove Mapping" link in matrix cells. |
| `CoursePrerequisiteController` | `POST` | `/api/v1/courses/{courseId}/prerequisites` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Add Prerequisite" modal in Course Catalog Manager. |
| `CoursePrerequisiteController` | `GET` | `/api/v1/courses/{courseId}/prerequisites` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Prerequisite chips and dependency list on course card. |
| `CoursePrerequisiteController` | `DELETE`| `/api/v1/courses/{courseId}/prerequisites/{prereqId}` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | Prerequisite chip removal action (`x` button). |

---

### 3.5 Grading Scales & Financial Catalog
*Controllers:* [`GradingScaleController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/GradingScaleController.java), [`FinancialController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/institution/FinancialController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GradingScaleController` | `GET` | `/api/v1/grading-scales` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Grading Scale table in Institution Management; rubric legends in Gradebook. |
| `GradingScaleController` | `GET` | `/api/v1/grading-scales/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Single grade band inspector. |
| `GradingScaleController` | `POST` | `/api/v1/grading-scales` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "New Grade Tier" button in Grading Scale Manager. |
| `GradingScaleController` | `PUT` | `/api/v1/grading-scales/{id}` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "Edit Range / Remarks" in Grading Scale Manager. |
| `GradingScaleController` | `DELETE`| `/api/v1/grading-scales/{id}` | `ADMIN` | `@PreAuthorize("hasAnyRole('ADMIN')")` | "Delete Scale Tier" button (Admin only). |
| `FinancialController` | `GET` | `/api/v1/fee-categories` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Fee category filter pills in Financial Foundations view. |
| `FinancialController` | `POST` | `/api/v1/fee-categories` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Add Fee Category" button in Financial Foundations. |
| `FinancialController` | `PUT` | `/api/v1/fee-categories/{id}` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Rename Category" inline action. |
| `FinancialController` | `DELETE`| `/api/v1/fee-categories/{id}` | `ADMIN` | `@PreAuthorize("hasAnyRole('ADMIN')")` | "Delete Category" button (Admin only). |
| `FinancialController` | `GET` | `/api/v1/fee-catalog` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Master Fee Catalog table (tuition rates, lab fees, miscellaneous). |
| `FinancialController` | `GET` | `/api/v1/fee-catalog/category/{categoryId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Filtered fee list for category. |
| `FinancialController` | `POST` | `/api/v1/fee-catalog` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Add Fee Item" dialog (FHE billable, CHED sanctioned flags). |
| `FinancialController` | `PUT` | `/api/v1/fee-catalog/{id}` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Update Fee Rate" dialog. |
| `FinancialController` | `DELETE`| `/api/v1/fee-catalog/{id}` | `ADMIN` | `@PreAuthorize("hasAnyRole('ADMIN')")` | "Delete Fee Item" button (Admin only). |
| `FinancialController` | `GET` | `/api/v1/payment-term-templates` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Installment breakdown table (downpayment, prelim, midterm, final). |
| `FinancialController` | `POST` | `/api/v1/payment-term-templates` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "New Installment Template" dialog. |
| `FinancialController` | `DELETE`| `/api/v1/payment-term-templates/{id}` | `ADMIN` | `@PreAuthorize("hasAnyRole('ADMIN')")` | "Delete Template" button (Admin only). |
| `FinancialController` | `GET` | `/api/v1/scholarship-discounts` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Scholarship & Discount Matrix table. |
| `FinancialController` | `POST` | `/api/v1/scholarship-discounts` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Create Scholarship Rule" dialog. |
| `FinancialController` | `DELETE`| `/api/v1/scholarship-discounts/{id}` | `ADMIN` | `@PreAuthorize("hasAnyRole('ADMIN')")` | "Delete Scholarship" button (Admin only). |

---

### 3.6 Class Scheduling & Faculty Workload
*Controller:* [`SchedulingController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/scheduling/SchedulingController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `SchedulingController` | `POST` | `/api/v1/scheduling/rooms` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "Add Room" modal in Timetable Scheduler. |
| `SchedulingController` | `GET` | `/api/v1/scheduling/rooms` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Room selector dropdown; conflict detector grid. |
| `SchedulingController` | `GET` | `/api/v1/scheduling/rooms/campus/{campusId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Filter rooms by selected campus. |
| `SchedulingController` | `POST` | `/api/v1/scheduling/sections` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Create Section" modal (Course, capacity, code). |
| `SchedulingController` | `GET` | `/api/v1/scheduling/sections/term/{termId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Master Section list for selected term. **Data Scoped:** `DEAN` (College programs), `CHAIRPERSON` (Assigned program), `FACULTY` (Assigned sections only). |
| `SchedulingController` | `GET` | `/api/v1/scheduling/sections/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Section detail card, enrolled count, timetable slots. |
| `SchedulingController` | `POST` | `/api/v1/scheduling/sections/{sectionId}/slots` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Assign Time Slot & Instructor" in Timetable Scheduler. |
| `SchedulingController` | `GET` | `/api/v1/scheduling/faculty-workload/term/{termId}/faculty/{facultyId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `FACULTY` | `@PreAuthorize("hasAnyRole(...)")` | Faculty Workload Bar / Gauge (Teaching load, overload indicator). |
| `SchedulingController` | `POST` | `/api/v1/scheduling/faculty-workload/approve-overload` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Approve Overload" button on faculty workload warning banner. |
| `SchedulingController` | `PUT` | `/api/v1/scheduling/faculty/{id}/workload-limit` | `ADMIN`, `DEAN` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")` | "Adjust Workload Limit" dialog (Max units, rationale). |
| `SchedulingController` | `GET` | `/api/v1/scheduling/terms` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Term selector in Scheduling module. |
| `SchedulingController` | `PUT` | `/api/v1/scheduling/terms/{termId}/max-class-hours` | `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` | "Configure Daily Class Hour Limit" policy setting. |
| `SchedulingController` | `GET` | `/api/v1/scheduling/instructors` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT` | `@PreAuthorize("hasAnyRole(...)")` | Instructor assignment dropdown in Timetable Scheduler. |

---

### 3.7 Faculty Profile, Roster & Account Provisioning
*Controller:* [`FacultyProfileController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/faculty/FacultyProfileController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `FacultyProfileController` | `POST` | `/api/v1/faculty` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Create Faculty Account" modal in Faculty Management (`/dashboard/faculty-accounts`). Provisions `User`, links `FacultyProfile`, and supports College/Program attachment. |
| `FacultyProfileController` | `GET` | `/api/v1/faculty` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")` | Institutional Faculty Accounts directory table with College/Program columns. |
| `FacultyProfileController` | `GET` | `/api/v1/faculty/{userId}/profile` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` OR Authenticated Faculty Self | `@PreAuthorize("hasAnyRole(...) or @facultySecurity.isFacultySelf(#userId, authentication)")` | Faculty profile overview card, academic rank, tenure status, and College/Program affiliation. |
| `FacultyProfileController` | `PUT` | `/api/v1/faculty/{userId}/profile` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "Edit Faculty Profile" dialog (Rank, employment status, max units, College/Program affiliation). |
| `FacultyProfileController` | `GET` | `/api/v1/reports/ched-e5` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "Export CHED E-5 Teaching Load Report" button & table. |

---

### 3.8 Student Registry, Transferee Crediting & Clearance
*Controller:* [`StudentController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/enrollment/StudentController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `StudentController` | `POST` | `/api/v1/students` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Register New Student" wizard in Student Admissions view. |
| `StudentController` | `GET` | `/api/v1/students/{id}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY` OR Student Self | `@PreAuthorize("hasAnyRole(...) or @enrollmentSecurity.canAccessStudentAdvising(authentication, #id)")` | Student Profile header, curriculum progress overview. |
| `StudentController` | `GET` | `/api/v1/students/search` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")` | Student search bar in Enrollment & Advising dashboard. **Data Scoped:** `DEAN` (College programs), `CHAIRPERSON` (Assigned program). |
| `StudentController` | `PATCH`| `/api/v1/students/{id}/clearance` | `ADMIN`, `REGISTRAR`, `CASHIER` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'CASHIER')")` | Financial and Departmental clearance status toggle (`CLEARED`, `PENDING`, `BLOCKED`). |
| `StudentController` | `POST` | `/api/v1/students/{id}/credit-courses` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "Credit Transferee Courses" dialog; records previous institution equivalencies. |
| `StudentController` | `GET` | `/api/v1/students/{id}/credited-courses` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY` OR Student Self | `@PreAuthorize("hasAnyRole(...) or @enrollmentSecurity.canAccessStudentAdvising(authentication, #id)")` | "Credited Subjects" tab in Student Profile view. |
| `StudentController` | `GET` | `/api/v1/students/me` | `STUDENT` | `@PreAuthorize("hasRole('STUDENT')")` | Student self-service dashboard home; resolves authenticated student context. |

---

### 3.9 Enrollment, Advising & Section Enlistment
*Controller:* [`EnrollmentController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/enrollment/EnrollmentController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `EnrollmentController` | `GET` | `/api/v1/enrollment/advising/student/{studentId}/term/{termId}` | Institutional Staff (`ADMIN`, `REGISTRAR`, `DEAN`, `CHAIRPERSON`, `FACULTY`) OR Student Self | `@PreAuthorize("@enrollmentSecurity.canAccessStudentAdvising(authentication, #studentId)")` | Student Advising evaluation matrix, eligible subjects list, prerequisite block flags, clearance status badges. |
| `EnrollmentController` | `POST` | `/api/v1/enrollment/enlist/student/{studentId}` | Enlistment Staff (`ADMIN`, `REGISTRAR`, `DEAN`, `CHAIRPERSON`) OR Student Self | `@PreAuthorize("@enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")` | "Enlist Section" button on section cards. **Gated:** Enforces prerequisite completion, unit limit, and clearance status (`financialClearance == CLEARED` & `departmentalClearance == CLEARED`). |
| `EnrollmentController` | `DELETE`| `/api/v1/enrollment/enlist/student/{studentId}/term/{termId}/section/{sectionId}` | Enlistment Staff (`ADMIN`, `REGISTRAR`, `DEAN`, `CHAIRPERSON`) OR Student Self | `@PreAuthorize("@enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")` | "Drop Enlisted Section" button (`x` on enlisted timetable). |
| `EnrollmentController` | `POST` | `/api/v1/enrollment/confirm/student/{studentId}` | Enlistment Staff (`ADMIN`, `REGISTRAR`, `DEAN`, `CHAIRPERSON`) OR Student Self | `@PreAuthorize("@enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")` | "Confirm & Lock Enrollment" button (generates assessment slip). **Gated:** Re-verifies clearance flags. |
| `EnrollmentController` | `GET` | `/api/v1/enrollment/student/{studentId}/term/{termId}` | Enlistment Staff (`ADMIN`, `REGISTRAR`, `DEAN`, `CHAIRPERSON`) OR Student Self | `@PreAuthorize("@enrollmentSecurity.canAccessStudentEnrollment(authentication, #studentId)")` | Student's current enrollment slip and schedule preview. |
| `EnrollmentController` | `GET` | `/api/v1/enrollment/term/{termId}` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")` | University-wide enrollment roster table and audit reports. |
| `EnrollmentController` | `PUT` | `/api/v1/enrollment/{id}/status` | `ADMIN`, `DEAN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")` | "Change Enrollment Status" dropdown (OFFICIALLY_ENROLLED, DROPPED, CANCELLED). |

---

### 3.10 Gradebook, Verification & Sealing
*Controller:* [`GradeController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/grade/GradeController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GradeController` | `GET` | `/api/v1/sections/{id}/roster` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#id, authentication)")` | Section roster table with student list, midterm/final grade columns. |
| `GradeController` | `PUT` | `/api/v1/sections/{id}/grades` | `ADMIN`, `DEAN`, `CHAIRPERSON` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#id, authentication)")` | "Save Grades / Submit Gradesheet" button in Faculty Gradebook. |
| `GradeController` | `POST` | `/api/v1/sections/{id}/grades/verify` | `ADMIN`, `DEAN`, `CHAIRPERSON` | `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")` | "Verify Gradesheet" button (Departmental quality sign-off). |
| `GradeController` | `POST` | `/api/v1/sections/{id}/grades/seal` | `ADMIN`, `REGISTRAR` | `@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")` | "Seal Official Final Grades" button (Registrar permanent seal). |

---

### 3.11 Class Record & Continuous Assessment
*Controller:* [`ClassRecordController.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/controller/grade/ClassRecordController.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `ClassRecordController` | `GET` | `/api/v1/class-records/sections/{sectionId}/config` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#sectionId, authentication)")` | Class Record configuration drawer (weights for Quizzes, Exams, Labs). |
| `ClassRecordController` | `PUT` | `/api/v1/class-records/sections/{sectionId}/config` | `ADMIN`, `DEAN`, `CHAIRPERSON` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#sectionId, authentication)")` | "Save Assessment Weights" button. |
| `ClassRecordController` | `POST` | `/api/v1/class-records/sections/{sectionId}/items` | `ADMIN`, `DEAN`, `CHAIRPERSON` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#sectionId, authentication)")` | "Add Assessment Item" column button (e.g., "Quiz 1", 50 pts). |
| `ClassRecordController` | `DELETE`| `/api/v1/class-records/items/{itemId}` | `ADMIN`, `DEAN`, `CHAIRPERSON` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructorForItem(#itemId, authentication)")` | "Delete Assessment Item" column header action. |
| `ClassRecordController` | `GET` | `/api/v1/class-records/sections/{sectionId}/matrix` | `ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#sectionId, authentication)")` | Continuous Assessment Spreadsheet Matrix (all students x all items). |
| `ClassRecordController` | `POST` | `/api/v1/class-records/sections/{sectionId}/scores/batch` | `ADMIN`, `DEAN`, `CHAIRPERSON` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#sectionId, authentication)")` | "Save Scores" button / auto-save on spreadsheet cell blur. |
| `ClassRecordController` | `POST` | `/api/v1/class-records/sections/{sectionId}/recalculate` | `ADMIN`, `DEAN`, `CHAIRPERSON` OR Assigned Section Instructor | `@PreAuthorize("hasAnyRole(...) or @sectionSecurity.isInstructor(#sectionId, authentication)")` | "Recalculate & Sync Grades" button; updates section midterm/final scores. |

---

### 3.12 Operational Telemetry & Health Actuators
*Config:* [`ActuatorSecurityConfig.java`](file:///C:/Users/USER/Documents/Github/backend-repo/student-digital-twin-v1.0.0/student-digital-twin-v1.0.0-backend/src/main/java/com/sdt/web_app/config/ActuatorSecurityConfig.java)

| Controller / Feature Module | HTTP Method | Endpoint Path | Required Role(s) / Authority | Source Constraint | Frontend UI Impact |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Spring Boot Actuator | `GET` | `/actuator/health` | `permitAll()` (Public/Guest) | `ActuatorSecurityConfig` matcher | System status ping / footer connectivity monitor. |
| Spring Boot Actuator | `GET` | `/actuator/info` | `permitAll()` (Public/Guest) | `ActuatorSecurityConfig` matcher | Build version / Git commit info popup. |
| Spring Boot Actuator | `GET` | `/actuator/prometheus` | `MONITORING` | `hasRole("MONITORING")` | Grafana / Prometheus scraping (Backend only). |
| Spring Boot Actuator | `GET` | `/actuator/metrics` | `MONITORING` | `hasRole("MONITORING")` | Admin telemetry dashboard. |
| Spring Boot Actuator | `*` | `/actuator/**` | `INFRA_ADMIN` | `hasRole("INFRA_ADMIN")` | Low-level runtime controls (thread dumps, env, beans). |

---

## 4. Frontend Navigation & Route Guard Matrix

This matrix provides the exact configuration required for Angular 19+ route guards (`canActivate: [roleGuard([...])]`), dynamic sidebar visibility filters in `DashboardLayoutComponent`, and intake module routing.

### 4.1 Global Route Configuration Reference

```typescript
// Angular Route Guard Mapping (src/app/app.routes.ts)
export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent, canActivate: [guestGuard] },
  { path: 'reset-password', component: ResetPasswordComponent, canActivate: [guestGuard] },
  { path: 'forbidden', component: ForbiddenComponent, canActivate: [authGuard] },
  {
    path: 'dashboard',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: DashboardComponent },
      {
        path: 'institution',
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR'])],
        loadChildren: () => import('./features/institution/institution.routes').then(m => m.INSTITUTION_ROUTES)
      },
      {
        path: 'curriculum',
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])],
        loadChildren: () => import('./features/curriculum/curriculum.routes').then(m => m.CURRICULUM_ROUTES)
      },
      {
        path: 'scheduling',
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])],
        loadChildren: () => import('./features/scheduling/scheduling.routes').then(m => m.SCHEDULING_ROUTES)
      },
      {
        path: 'enrollment',
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT'])],
        loadChildren: () => import('./features/enrollment/enrollment.routes').then(m => m.ENROLLMENT_ROUTES)
      },
      {
        path: 'grades',
        canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])],
        loadComponent: () => import('./features/gradebook/faculty-gradebook.component').then(m => m.FacultyGradebookComponent)
      },
      {
        path: 'users',
        canActivate: [roleGuard(['ADMIN', 'REGISTRAR'])],
        loadComponent: () => import('./features/admin/user-management/user-management.component').then(m => m.UserManagementComponent)
      },
      {
        path: 'faculty-accounts',
        canActivate: [roleGuard(['ADMIN', 'REGISTRAR'])],
        loadComponent: () => import('./features/faculty-management/faculty-management.component').then(m => m.FacultyManagementComponent)
      }
    ]
  }
];
```

---

### 4.2 Role-Specific Access, Scoping & Intake Specifications

#### 1. `ADMIN` (Institutional Administrator)
- **Scope:** System-wide, unrestricted across all campuses, colleges, departments, and programs.
- **Intake Module:** **User Accounts** (`/dashboard/users`, `/api/v1/users`) & **Faculty Accounts** (`/dashboard/faculty-accounts`, `/api/v1/faculty`).
  - Full CRUD on application users, role assignments (`ADMIN`, `DEAN`, `CHAIRPERSON`, `REGISTRAR`, `FACULTY`, `STUDENT`, `CASHIER`, `GUIDANCE`), College and Program attachment, password resets, and activation/deactivation.
  - Hard deletion authority on user records (`DELETE /api/v1/users/{id}`).
- **Allowed Modules:**
  - Overview: Dashboard, Academic Twin.
  - Academics: Institutional Registry, Curriculum Designer, Class Scheduling, Enrollment & Advising, Faculty Gradebook, Courses & Enrolled, Class Schedule, Grades & Progress, Attendance.
  - Administration: User Accounts, Faculty Accounts.
  - Student Services: Clearance & Billing, Digital Student ID, ICT Helpdesk.
- **Granular Action Permissions:**
  - Full CRUD on Campuses, Departments, Programs, Academic Years, Terms, and Fee Catalogs.
  - Hard deletion authority on root master entities.
  - Departmental and Financial clearance overrides (`PATCH /api/v1/students/{id}/clearance`).
  - Verify and Seal any section grade sheet.

#### 2. `REGISTRAR` (University Registrar)
- **Scope:** Institutional-wide academic and enrollment scope across all programs.
- **Intake Module:** **Faculty Accounts** (`/dashboard/faculty-accounts`, `POST /api/v1/faculty`) & **User Accounts** (`/dashboard/users`, `/api/v1/users`).
  - Provisions new faculty users with default temporary credentials and automatically creates/links their `FacultyProfile` records, attaching College and Program.
  - Provisions and updates application users (`/dashboard/users`), assigning roles and attaching College and Program affiliations. User deletion is reserved strictly for `ADMIN`.
- **Allowed Modules:**
  - Overview: Dashboard.
  - Academics: Institutional Registry, Class Scheduling, Enrollment & Advising, Faculty Gradebook, ICT Helpdesk.
  - Administration: Faculty Accounts, User Accounts.
- **Granular Action Permissions:**
  - Master Academic Calendar controls (Years, Terms, Enrollment Windows, Grading Windows, Add/Drop Periods).
  - Student Admissions intake (`POST /api/v1/students`).
  - Transferee crediting (`POST /api/v1/students/{id}/credit-courses`).
  - Student clearance management (`PATCH /api/v1/students/{id}/clearance`).
  - User account management and College/Program attachment.
  - Enlistment overrides & status lifecycle changes (OFFICIALLY_ENROLLED, DROPPED).
  - Permanent official grade sealing (`POST /api/v1/sections/{id}/grades/seal`).
  - **Restricted:** Cannot verify grades (academic peer review assigned to Dean/Chairperson); cannot delete users (strictly `ADMIN`).

#### 3. `DEAN` (College Dean)
- **Scope:** Scoped strictly to assigned College and all child departments (`departmentRepository.findByParentDepartmentId(...)`).
- **Intake Review:** Read-only access to Faculty Accounts directory (`GET /api/v1/faculty`).
- **Allowed Modules:**
  - Overview: Dashboard.
  - Academics: Institutional Registry, Curriculum Designer, Class Scheduling, Enrollment & Advising, Faculty Gradebook, ICT Helpdesk.
- **Data Scoping & Filtering (`DataScopingService`):**
  - Section listings and timetable schedules automatically filtered to programs under the Dean's college hierarchy.
  - Student searches in enrollment/advising filtered to students in the college's programs.
- **Granular Action Permissions:**
  - Create & edit departments, programs, and PILOs under their college.
  - Create and transition curricula states (Draft -> Submitted -> Approved).
  - Build class sections, timetable slots, and assign faculty.
  - Approve faculty overloads (`POST /api/v1/scheduling/faculty-workload/approve-overload`) and adjust workload limits.
  - Verify submitted gradesheets (`POST /api/v1/sections/{id}/grades/verify`).
  - Export CHED E-5 Faculty Load Reports.
  - **Restricted:** Cannot seal grades (Registrar only); cannot delete Campuses, Academic Years, or Terms; cannot access User Accounts intake.

#### 4. `CHAIRPERSON` (Department Chairperson)
- **Scope:** Scoped strictly to assigned Program (`Program.chairpersonUserId = currentUserId`).
- **Intake Review:** Read-only access to Faculty Accounts directory (`GET /api/v1/faculty`).
- **Allowed Modules:**
  - Overview: Dashboard.
  - Academics: Institutional Registry (Courses & CILO-PILO Matrix tabs), Curriculum Designer, Class Scheduling, Enrollment & Advising, Faculty Gradebook, ICT Helpdesk.
- **Data Scoping & Filtering (`DataScopingService`):**
  - Section listings and timetable schedules restricted strictly to their assigned degree program.
  - Student search in enrollment filtered to students belonging to their program.
- **Granular Action Permissions:**
  - Curriculum Designer: Add/remove courses, position nodes, configure prerequisites, manage CILO-PILO mappings.
  - Create class sections and timetable slots for their program.
  - Verify faculty gradesheets within their program (`POST /api/v1/sections/{id}/grades/verify`).
  - **Restricted:** Cannot approve faculty overloads; cannot seal grades; cannot modify fee catalogs or academic terms; cannot access User Accounts intake.

#### 5. `FACULTY` (Teaching Staff / Instructor)
- **Scope:** Scoped strictly to active teaching load and assigned sections.
- **Allowed Modules:**
  - Overview: Dashboard, Academic Twin.
  - Academics: Faculty Gradebook, Class Schedule (read-only), ICT Helpdesk.
- **Data Scoping & UI Capabilities:**
  - Gradebook section selector filters to sections where the user is `primaryInstructor` or assigned in timetable slots.
  - Term switcher supports Active Term vs. Historical Terms filtering.
  - Full Continuous Assessment control: Configure grading weights, create assessment items, enter raw matrix scores, and trigger score recalculation.
  - Submit gradesheets for departmental verification (`PUT /api/v1/sections/{id}/grades`).
  - **Restricted:** Blocked from creating sections, modifying curricula, verifying/sealing grades, and accessing user/faculty intakes.

#### 6. `STUDENT` (Enrolled Student)
- **Scope:** Strictly isolated to own student profile context (`/api/v1/students/me`, `canAccessStudentEnrollment`).
- **Allowed Modules:**
  - Overview: Dashboard, Academic Twin.
  - Academics: Enrollment & Advising (Self-Service), Courses & Enrolled, Class Schedule, Grades & Progress (Personal).
  - Student Services: Clearance & Billing, Digital Student ID, ICT Helpdesk.
- **Self-Enrollment Gate Enforcement:**
  - Financial Clearance: Must have `financialClearance == 'CLEARED'`.
  - Departmental Clearance: Must have `departmentalClearance == 'CLEARED'`.
  - Prerequisite Audit: Prerequisite subjects must be completed and passed.
  - Unit Limit: Enlisted units cannot exceed term limit (e.g. 24 units).
  - Self-enlistment controls are locked and visual warning alerts are displayed if clearance flags are `PENDING` or `BLOCKED`.
- **Restricted:** Completely blocked from administrative views (User Management, Faculty Accounts, Institution Management, Timetable Builder, Gradebook scoring).

#### 7. `CASHIER` (Finance & Billing Staff)
- **Scope:** Student financial accounts, fee catalogs, and student clearances.
- **Allowed Modules:**
  - Overview: Dashboard.
  - Student Services: Clearance & Billing, Fee Catalog lookup.
- **Granular Action Permissions:**
  - Toggle student financial and departmental clearance status (`PATCH /api/v1/students/{id}/clearance`).
  - Assess student tuition and fee statements.

#### 8. `GUIDANCE` (Guidance Counselor)
- **Scope:** Behavioral analytics, academic advising history, student twin progression.
- **Allowed Modules:**
  - Overview: Dashboard, Academic Twin.
  - Academics: Enrollment & Advising (Read-only student advising history), Student Progress.

#### 9. `GUEST` (Unauthenticated Visitor)
- **Scope:** Public authentication gateway.
- **Accessible Routes:** `/login`, `/forgot-password`, `/reset-password`.
- **Action Permissions:** Submit login credentials, request password reset link, complete password reset with token.

---

## 5. Security Remediation & Architecture Verification

### 5.1 Completed Architectural Hardening

1. **Cleaned Boilerplate Matcher in `WebSecurityConfig`:**
   - Removed dead `/api/orders/**` rule. Global API security is handled via order-specific route matchers and `@EnableMethodSecurity`.

2. **JWT User Identity Extraction (`sub` Claim):**
   - Updated `AuthService` in Angular frontend to extract `sub` as numeric `id` and expose `currentUser` signal with complete ID context, resolving instructor ownership comparisons in the Gradebook.

3. **Dynamic Sidebar Navigation (`filteredNavSections`):**
   - Implemented reactive filtering in `DashboardLayoutComponent` using an Angular computed signal based on `currentUser().role`. Administrative modules (User Accounts, Faculty Accounts, Institutional Registry, Timetable Builder) are completely hidden from unauthorized roles.

4. **Advising & Enlistment Privilege Gate Hardening:**
   - Enlistment and confirming endpoints in `EnrollmentService` enforce financial and departmental clearance checks.
   - Student Advising view displays clear badges for Financial and Departmental clearance, rendering a prominent warning alert when self-enrollment is locked due to pending clearance.
   - Guarded Admissions Intake ("Admit Student") and Transferee Crediting buttons with `@if (canAdmit())` and `@if (canCredit())`.

5. **Multi-Tier Departmental Scoping (`DataScopingService`):**
   - Implemented `DataScopingService` in backend.
   - Integrated scoping into `SchedulingService.getSectionsForTerm(...)` and `StudentService.searchStudents(...)`.

---

## 6. Implementation Checklist & Verification Status

- [x] **Dynamic Navigation Filtering:** In `dashboard-layout.component.ts`, `filteredNavSections` dynamically filters menu sections against `currentUser().role`.
- [x] **Route Guard Alignment:** Every route in `app.routes.ts` and feature routes strictly aligns with the institutional RBAC matrix, including `/dashboard/users` (`ADMIN`, `REGISTRAR`) and `/dashboard/faculty-accounts` (`ADMIN`, `REGISTRAR`).
- [x] **Action Button Conditional Visibility (`@if`):**
  - "Verify Grades" button -> `@if (canVerifyGrades())` (`ADMIN`, `DEAN`, `CHAIRPERSON`)
  - "Seal Grades" button -> `@if (canSealGrades())` (`ADMIN`, `REGISTRAR`)
  - "Approve Overload" button -> `@if (canApproveOverload())` (`ADMIN`, `DEAN`)
  - "Admit Student" -> `@if (canAdmit())` (`ADMIN`, `REGISTRAR`)
  - "Credit Transferee" -> `@if (canCredit())` (`ADMIN`, `DEAN`, `REGISTRAR`)
  - "Recalculate & Sync Grades" -> `@if (isInstructor() || canVerifyGrades())`
- [x] **HTTP 403 Forbidden Handling:** Global HTTP error interceptor captures `403 Forbidden` errors and redirects to `/forbidden` or shows toast alerts without session termination.
- [x] **User Accounts Intake Module:** Created full-stack user management feature for `ADMIN` & `REGISTRAR` (`/api/v1/users`, `/dashboard/users`), allowing deletion solely by `ADMIN`.
- [x] **Faculty Accounts Intake Module:** Created full-stack faculty management feature for `REGISTRAR` & `ADMIN` (`/api/v1/faculty`, `/dashboard/faculty-accounts`).
- [x] **Institutional College & Program Attachment:** Both `ADMIN` and `REGISTRAR` can attach and update College (`Department`) and `Program` linkages during User Account and Faculty Account provisioning, with strict cascading selection and role scoping validations (`DEAN` college-only; `CHAIRPERSON` college + program).
- [x] **Departmental Data Scoping:** Implemented college and program level scoping for `DEAN` and `CHAIRPERSON` across scheduling and student queries.
- [x] **Clearance & Enlistment Gates:** Enforced `financialClearance` and `departmentalClearance` checks across backend services and frontend UI.
