# REMEDIATION PLAN: Cross-Module System Remediation & Invariant Hardening

**Student Digital Twin v1.0.0** **Spring Boot 4.1.0 / Java 21 LTS · Angular 19+ / PrimeNG 21+ · Flyway V1–V12** **Document Version:** 1.0.0 · **Date:** 2026-09-08 **Audited Directives:** `00 AI Index.md` & `/00 AI Audit/`

---

## Executive Summary & Blast Radius Map

Following an exhaustive inspection of `00 AI Index.md` and the three primary audit artifacts in `/00 AI Audit/` (_Comprehensive System Gap Analysis & Gate Audit Report_, _Backend File Relationships & Architecture Topology_, and _Frontend Component Hierarchy & State Matrix_), the codebase is currently at **129/129 passing backend tests** and **83/83 passing frontend tests**.

However, several critical structural, transactional, authorization, and reactive state invariants require surgical remediation before production deployment.

---

## Phase 1: Context & Dependency Analysis

### 1. Cross-Module Impact & Contract Analysis

1. **Security & Identity (Principal Leakage & Fallback IDs)**:
    
    - `SchedulingController` currently resolves `approverId` via `securityUtils.resolveUserId(authentication)`. If `null`, it silently falls back to `1L` (the seeded system admin). This creates a data forgery risk and corrupts audit trails under unauthenticated or misconfigured requests.
    - `PasswordResetService` stores raw UUID tokens in plaintext and logs them at `INFO` and `WARN` levels. Anyone with read access to the database or production log stream (CloudWatch, Datadog) can take over any user account.
    - `TokenService` issues JWTs with `api://web-app` audience while `WebSecurityConfig` defaults to `api://sdt-webapp`. The primary resource server decoder validates `api://sdt-webapp`, creating potential validation rejection when using production JWKS decoders.
2. **Curriculum Design (State Machine & Workflow Isolation)**:
    
    - `Curriculum.transitionTo()` only blocks exits from `ACTIVE` to non-`ARCHIVED`. Invalid transitions (such as `ARCHIVED -> DRAFT`, `DRAFT -> ACTIVE` without review, or regression from `APPROVED`) silently succeed.
    - `CurriculumController` excludes `CHAIRPERSON` from `POST /{id}/validate` and `POST /{id}/transition-state`, while permitting chairpersons to assemble courses and prerequisites. Chairpersons are deadlocked from advancing their own drafts.
3. **Facilities & Physical Scheduling (Gate 2 Invariants)**:
    
    - `SchedulingService.createSection()` and `addScheduleSlots()` enforce room collision and session duration caps, but completely omit room physical capacity verification (`room.capacity >= section.maxCapacity`). A 50-student section can currently be assigned to a 20-seat laboratory.
4. **Concurrency & First-Level JPA Cache (Gate 3 Invariants)**:
    
    - `ClassSectionRepository` declares `@Modifying` queries for `incrementEnrolledCountIfOpen` and `decrementEnrolledCount` without `clearAutomatically = true`. When a `ClassSection` entity is loaded prior to enlistment in `EnrollmentService.enlistSection()`, Hibernate's persistence context retains stale `enrolledCount` data.
    - `TransfereeCreditingService` creates `StudentCourseGrade` entities for accredited courses and updates `totalUnitsEarned`, but passes the student's stale `cumulativeGpa` without recomputing weighted GPA.
5. **Frontend State & Route Protection**:
    
    - `/dashboard/grades` in `app.routes.ts` lacks `roleGuard`, exposing the faculty gradebook UI to authenticated students.
    - `EnrollmentStore.studentId` is hardcoded to `signal<number>(1)`. When a student logs in, the store defaults to student 1 instead of resolving the student's authentic profile.
    - `EnrollmentStore.loadStudentAdvising` and `SchedulingStore.loadInitialData` fire multiple uncoordinated subscriptions where premature `finalize()` calls clear `isLoading` while parallel requests are still in-flight.

---

## Phase 2: Categorized Issue Inventory

### Tier 1: P0 Blockers (Security, Identity & Data Integrity)

|ID|Subsystem|Target File|Core Flaw|Blast Radius|
|---|---|---|---|---|
|**P0-1**|Security|`SchedulingController.java`|Hardcoded `approverId = 1L` fallback when principal resolution fails|Any invalid or anonymous request attributes overload approvals to User 1|
|**P0-2**|Security|`PasswordResetService.java`|Raw UUID tokens stored unhashed; reset URLs printed to logs at `INFO` and `WARN`|Database exposure or log shipping leaks live reset tokens|
|**P0-3**|Routing|`app.routes.ts`|`/dashboard/grades` has no `roleGuard`, permitting student access|Unauthorized users navigate to the Gradebook UI|
|**P0-4**|State|`enrollment.store.ts`|`studentId = signal<number>(1)` hardcoded default|Cold load immediately queries Student 1's records regardless of logged-in user|
|**P0-5**|Data Cache|`ClassSectionRepository.java`|`@Modifying` without `clearAutomatically = true`|Stale entity state in Hibernate persistence context during multi-step enrollment transactions|

### Tier 2: P1 Core Invariants (Domain Rules & Business Logic)

|ID|Subsystem|Target File|Core Flaw|Blast Radius|
|---|---|---|---|---|
|**P1-1**|Scheduling|`SchedulingService.java`|No room physical capacity check against section `maxCapacity`|Physical overflow: larger classes booked in undersized rooms|
|**P1-2**|Academic|`Curriculum.java`|`transitionTo()` lacks directed-graph allowed transition map|Illegal state jumps (`ARCHIVED -> DRAFT`, `DRAFT -> ACTIVE` skipping review)|
|**P1-3**|Workflow|`CurriculumController.java`|`CHAIRPERSON` excluded from `validate` and `transition-state`|Department chairs cannot validate or advance their curriculum versions|
|**P1-4**|Enrollment|`TransfereeCreditingService.java`|Pass-through of stale `cumulativeGpa` during course crediting|Transferees display incorrect 0.00 or stale GPA after receiving credits|
|**P1-5**|Student Self|`StudentController.java` & `StudentService.java`|Missing `GET /api/v1/students/me` endpoint|Students have no direct way to query their own profile ID without calling `/search` (which is restricted to staff)|
|**P1-6**|Reactivity|`enrollment.store.ts`|Two uncoordinated subscriptions in `loadStudentAdvising`|Loading indicator clears prematurely; state race condition|
|**P1-7**|Reactivity|`scheduling.store.ts`|5 uncoordinated subscriptions in `loadInitialData`|Premature `isLoading.set(false)` when `programService` finishes first|
|**P1-8**|Security Config|`TokenService.java`|Audience mismatch (`api://web-app` vs `api://sdt-webapp`)|Token validation rejections when using strict resource server validators|

### Tier 3: P2 Polish & Resilience

|ID|Subsystem|Target File|Core Flaw|Blast Radius|
|---|---|---|---|---|
|**P2-1**|Error Handling|`global-error.interceptor.ts`|Missing handlers for `status === 0` (network failure/CORS) and `status === 404`|Silent or generic failure on connection drops|
|**P2-2**|Token Response|`PasswordResetService.java`|`PasswordResetResult` exposes `userId` in API response|Internal ID enumeration|

---

## Phase 3: File-by-File Implementation Breakdown

### 1. Backend Core & Security

#### File 1.1: `com/sdt/web_app/controller/scheduling/SchedulingController.java`

- **Current Flaw**: Lines 98–101 and 112–115 silently fall back to `approverId = 1L` / `adminUserId = 1L`.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: Long approverId = securityUtils.resolveUserId(authentication);  
    if (approverId == null) {  
        throw new AccessDeniedException("Unable to resolve authenticated administrator identity.");  
    }
    
- **Downstream Impact**: Tests asserting overload approval must ensure a valid mock `Authentication` / JWT principal is provided.

#### File 1.2: `com/sdt/web_app/service/authentication/PasswordResetService.java`

- **Current Flaw**: Tokens are stored as raw plaintext strings; reset URLs are logged unconditionally.
- **Proposed Logic**:
    - Hash token before saving:
        
        No diagram type detected matching given configuration for text: String rawToken = UUID.randomUUID().toString();  
        String hashedToken = DigestUtils.sha256Hex(rawToken);  
        PasswordResetToken resetToken = PasswordResetToken.createTokenForUser(hashedToken, user, EXPIRATION_MINUTES);  
        passwordResetTokenRepository.save(resetToken);
        
    - Look up using the hash:
        
        No diagram type detected matching given configuration for text: String hashedToken = DigestUtils.sha256Hex(rawToken);  
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(hashedToken)  
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token."));
        
    - Sanitize logging: remove raw `resetLink` from `INFO` and `WARN` logs.
    - In `PasswordResetResult`: remove `userId`, returning only `username` and `email`.
- **Downstream Impact**: `PasswordResetController` and unit tests in `AuthenticationIntegrationTest` will verify hashed token lookup.

#### File 1.3: `com/sdt/web_app/service/authentication/TokenService.java`

- **Current Flaw**: Defaults fallback audience to `api://web-app` instead of `api://sdt-webapp`.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: @Value("${spring.security.oauth2.resourceserver.jwt.audiences:api://sdt-webapp}")  
    private List<String> audiences;
    
- **Downstream Impact**: Aligns with `application.yml` and `WebSecurityConfig.java`.

---

### 2. Academic & Scheduling Engine

#### File 2.1: `com/sdt/web_app/entities/institution/Curriculum.java`

- **Current Flaw**: `transitionTo()` only blocks transitions leaving `ACTIVE`. Allows arbitrary state regression or invalid leaps.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = Map.of(  
        Status.DRAFT, Set.of(Status.UNDER_REVIEW),  
        Status.UNDER_REVIEW, Set.of(Status.DRAFT, Status.APPROVED),  
        Status.APPROVED, Set.of(Status.ACTIVE, Status.UNDER_REVIEW),  
        Status.ACTIVE, Set.of(Status.ARCHIVED),  
        Status.ARCHIVED, Set.of() // Terminal state  
    );  
      
    public void transitionTo(Status newStatus) {  
        if (this.status == newStatus) return;  
        Set<Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(this.status, Set.of());  
        if (!allowed.contains(newStatus)) {  
            throw new IllegalStateException(String.format(  
                "Invalid curriculum state transition from %s to %s.", this.status, newStatus));  
        }  
        this.status = newStatus;  
    }
    
- **Downstream Impact**: Guarantees workflow integrity in `CurriculumDesignerService`.

#### File 2.2: `com/sdt/web_app/controller/institution/CurriculumController.java`

- **Current Flaw**: Lines 121 and 127 omit `'CHAIRPERSON'` from `@PreAuthorize`.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: @PostMapping("/{id}/validate")  
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")  
    public ResponseEntity<ValidationReportDto> validateCurriculum(@PathVariable("id") Long id) { ... }  
      
    @PostMapping("/{id}/transition-state")  
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")  
    public ResponseEntity<Void> transitionState(...) { ... }
    
- **Downstream Impact**: Chairpersons can independently validate drafts and transition them to `UNDER_REVIEW`.

#### File 2.3: `com/sdt/web_app/service/scheduling/SchedulingService.java`

- **Current Flaw**: Does not check room capacity against section capacity during section creation or slot assignment.
- **Proposed Logic**: In `createSection()` (and `addScheduleSlots()`):
    
    No diagram type detected matching given configuration for text: Room room = roomRepository.findById(slot.roomId())  
        .orElseThrow(() -> new EntityNotFoundException("Room not found: " + slot.roomId()));  
      
    if (room.getCapacity() < request.maxCapacity()) {  
        throw new IllegalArgumentException(String.format(  
            "Gate 2 Violation: Room '%s' capacity (%d) is insufficient for section max capacity (%d).",  
            room.getCode(), room.getCapacity(), request.maxCapacity()));  
    }
    
- **Downstream Impact**: Prevents room overbooking beyond physical seating limits.

---

### 3. Enrollment & Student Records

#### File 3.1: `com/sdt/web_app/repositories/scheduling/ClassSectionRepository.java`

- **Current Flaw**: Lines 34 and 45 declare `@Modifying` without `clearAutomatically = true`.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: @Modifying(clearAutomatically = true)  
    @Query(...)  
    int incrementEnrolledCountIfOpen(@Param("id") Long id);  
      
    @Modifying(clearAutomatically = true)  
    @Query(...)  
    int decrementEnrolledCount(@Param("id") Long id);
    
- **Downstream Impact**: Evicts stale first-level Hibernate entity caches upon capacity modification.

#### File 3.2: `com/sdt/web_app/service/enrollment/TransfereeCreditingService.java`

- **Current Flaw**: Line 94 calls `student.updateProgress(totalUnitsEarned, student.getCumulativeGpa())`, preserving stale GPA.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: List<StudentCourseGrade> allPassed = studentCourseGradeRepository.findPassedGradesByStudentId(student.getId());  
    BigDecimal totalUnitsEarned = BigDecimal.ZERO;  
    BigDecimal weightedGradeSum = BigDecimal.ZERO;  
      
    for (StudentCourseGrade g : allPassed) {  
        BigDecimal units = g.getCourse().getCreditUnits();  
        totalUnitsEarned = totalUnitsEarned.add(units);  
        if (g.getNumericalGrade() != null) {  
            weightedGradeSum = weightedGradeSum.add(g.getNumericalGrade().multiply(units));  
        }  
    }  
      
    BigDecimal cumulativeGpa = totalUnitsEarned.compareTo(BigDecimal.ZERO) > 0  
            ? weightedGradeSum.divide(totalUnitsEarned, 2, RoundingMode.HALF_UP)  
            : null;  
      
    student.updateProgress(totalUnitsEarned, cumulativeGpa);  
    studentProfileRepository.save(student);
    
- **Downstream Impact**: Transferee students immediately reflect their true weighted GPA.

#### File 3.3: `com/sdt/web_app/controller/enrollment/StudentController.java` & `StudentService.java`

- **Current Flaw**: No self-lookup endpoint for students (`/search` is restricted to staff to prevent student registry enumeration under RA 10173).
- **Proposed Logic**: In `StudentController`:
    
    No diagram type detected matching given configuration for text: @GetMapping("/me")  
    @PreAuthorize("hasRole('STUDENT')")  
    public ResponseEntity<StudentProfileResponse> getCurrentStudentProfile(Authentication authentication) {  
        Long userId = securityUtils.resolveUserId(authentication);  
        return ResponseEntity.ok(studentService.getStudentByUserId(userId));  
    }
    
    In `StudentService`:
    
    No diagram type detected matching given configuration for text: @Transactional(readOnly = true)  
    public StudentProfileResponse getStudentByUserId(Long userId) {  
        StudentProfile sp = studentProfileRepository.findByUserIdWithProgramAndCurriculum(userId)  
                .orElseThrow(() -> new EntityNotFoundException("No student profile registered for user id: " + userId));  
        return mapToProfileResponse(sp);  
    }
    
- **Downstream Impact**: Enables the frontend to dynamically resolve the logged-in student's ID without hardcoding.

---

### 4. Frontend Routes & Signal Stores

#### File 4.1: `student-digital-twin-v1.0.0-frontend/src/app/app.routes.ts`

- **Current Flaw**: Line 104 maps `/dashboard/grades` without `roleGuard`.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: {  
      path: 'grades',  
      loadComponent: () =>  
        import('./features/gradebook/faculty-gradebook.component').then(m => m.FacultyGradebookComponent),  
      canActivate: [roleGuard(['ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY'])],  
      title: 'Faculty Gradebook'  
    }
    
- **Downstream Impact**: Student users attempting to navigate to `/dashboard/grades` are redirected to `/dashboard`.

#### File 4.2: `student-digital-twin-v1.0.0-frontend/src/app/features/enrollment/state/enrollment.store.ts`

- **Current Flaw**:
    1. `studentId` initialized to `signal<number>(1)`.
    2. `loadStudentAdvising()` fires two separate subscriptions with uncoordinated `finalize()`.
- **Proposed Logic**:
    1. Initialize `studentId = signal<number | null>(null)`.
    2. Replace uncoordinated calls in `loadStudentAdvising` with `forkJoin`:
        
        No diagram type detected matching given configuration for text: loadStudentAdvising(studentId: number, termId: number): void {  
          this.isLoading.set(true);  
          this.errorMessage.set(null);  
          
          forkJoin({  
            advising: this.enrollmentApi.getAdvisingEligibility(studentId, termId).pipe(  
              catchError(err => {  
                this.errorMessage.set(err.error?.detail || 'Failed to load advising checklist.');  
                return of(null);  
              })  
            ),  
            enrollment: this.enrollmentApi.getEnrollment(studentId, termId).pipe(  
              catchError(() => of(null))  
            )  
          }).pipe(  
            tap(({ advising, enrollment }) => {  
              this.advising.set(advising);  
              this.enrollment.set(enrollment);  
            }),  
            finalize(() => this.isLoading.set(false))  
          ).subscribe();  
        }
        
- **Downstream Impact**: Eliminates loading state flickering and displays the correct student profile.

#### File 4.3: `student-digital-twin-v1.0.0-frontend/src/app/features/scheduling/state/scheduling.store.ts`

- **Current Flaw**: `loadInitialData()` executes 5 uncoordinated parallel subscriptions, clearing `isLoading` on `programService` completion only.
- **Proposed Logic**: Coordinate initial catalog lookups with `forkJoin`:
    
    No diagram type detected matching given configuration for text: loadInitialData(): void {  
      this.isLoading.set(true);  
      forkJoin({  
        terms: this.schedulingApi.getSchedulingTerms().pipe(catchError(() => of([]))),  
        curricula: this.curriculumApi.getCurriculumLookupOptions().pipe(catchError(() => of([]))),  
        rooms: this.schedulingApi.getAllRooms().pipe(catchError(() => of([]))),  
        instructors: this.schedulingApi.getAvailableInstructors().pipe(catchError(() => of([]))),  
        programs: this.programService.getAll().pipe(catchError(() => of([])))  
      }).pipe(  
        tap(({ terms, curricula, rooms, instructors, programs }) => {  
          this.terms.set(terms);  
          this.curricula.set(curricula);  
          this.rooms.set(rooms);  
          this.instructors.set(instructors);  
          this.programs.set(programs);  
      
          if (terms.length > 0 && !this.selectedTermId()) {  
            const activeTerm = terms.find(t => t.isActive) || terms.find(t => t.isCurrent) || terms[0];  
            if (activeTerm) {  
              this.selectedTermId.set(activeTerm.id);  
              this.loadSections(activeTerm.id);  
            }  
          }  
          if (curricula.length > 0 && !this.selectedCurriculumId()) {  
            const activeCurr = curricula.find(c => c.status === 'ACTIVE') || curricula[0];  
            if (activeCurr) this.selectedCurriculumId.set(activeCurr.id);  
          }  
        }),  
        finalize(() => this.isLoading.set(false))  
      ).subscribe();  
    }
    
- **Downstream Impact**: Clean atomic initialization without race conditions or premature spinner clearance.

#### File 4.4: `student-digital-twin-v1.0.0-frontend/src/app/core/interceptors/error/global-error.interceptor.ts`

- **Current Flaw**: Ignores `status === 0` (network disconnection or CORS failure) and `status === 404`.
- **Proposed Logic**:
    
    No diagram type detected matching given configuration for text: if (error.status === 0) {  
      messageService?.add({  
        severity: 'error',  
        summary: 'Connection Failure',  
        detail: 'Unable to reach backend services. Please check network connectivity.',  
        life: 5000  
      });  
    } else if (error.status === 404) {  
      messageService?.add({  
        severity: 'warn',  
        summary: 'Not Found',  
        detail: error.error?.detail || 'The requested resource was not found.',  
        life: 4000  
      });  
    }
    

---

## Phase 4: Strictly Ordered Execution Sequence

To ensure zero compilation errors and continuous test passing, code modifications must proceed in the following strict order:

```
Step 1: Domain Entities & Repositories (Backend)
  ├── 1.1 Curriculum.java (Add ALLOWED_TRANSITIONS state machine)
  └── 1.2 ClassSectionRepository.java (Add clearAutomatically = true)

Step 2: Core Domain Services (Backend)
  ├── 2.1 PasswordResetService.java (SHA-256 token hashing & log sanitization)
  ├── 2.2 TokenService.java (Align audience to api://sdt-webapp)
  ├── 2.3 SchedulingService.java (Room capacity vs section maxCapacity guard)
  ├── 2.4 TransfereeCreditingService.java (Weighted GPA recalculation)
  └── 2.5 StudentService.java (Implement getStudentByUserId)

Step 3: Security & REST Controllers (Backend)
  ├── 3.1 SchedulingController.java (Eliminate approverId = 1L fallback)
  ├── 3.2 CurriculumController.java (Add CHAIRPERSON to validate and transition-state)
  └── 3.3 StudentController.java (Add GET /api/v1/students/me)

Step 4: Backend Verification Gate
  └── Execute: .\mvnw.cmd test (Ensure all 129+ tests pass)

Step 5: Frontend Security & Routes
  ├── 5.1 app.routes.ts (Wire roleGuard on /dashboard/grades)
  └── 5.2 global-error.interceptor.ts (Add status 0 and 404 handlers)

Step 6: Frontend Reactive Signal Stores
  ├── 6.1 enrollment.store.ts (Nullable studentId & forkJoin coordination)
  └── 6.2 scheduling.store.ts (forkJoin in loadInitialData)

Step 7: Frontend Verification Gate & Production Build
  ├── 7.1 Execute: npx ng test --watch=false (Ensure all 83+ tests pass)
  └── 7.2 Execute: npx ng build (Verify zero build/budget warnings)
```

---

## Phase 5: Validation & Testing Checklist

### 1. Automated Regression Verification

- [ ] **Backend Test Suite**:
    - Run: `.\mvnw.cmd test`
    - Verify 129+ tests execute with 0 failures and 0 errors.
    - Assert `SchedulingServiceTest`, `EnrollmentSecurityTest`, `EnrollmentServiceTest`, `TransfereeCreditingServiceTest`, and `InstitutionalCrudPrerequisiteValidationTest` pass cleanly.
- [ ] **Frontend Vitest Suite**:
    - Run: `npx ng test --watch=false`
    - Verify 83+ tests across all 35 test files pass with 0 failures.
- [ ] **Production Build Check**:
    - Run: `npx ng build`
    - Verify clean bundle generation within style and budget constraints.

### 2. Specific Behavioral Assertions

- [ ] **State Machine Enforcement**:
    - Attempt `ARCHIVED -> DRAFT` on a curriculum; assert `IllegalStateException` is thrown.
    - Attempt `DRAFT -> ACTIVE` directly; assert `IllegalStateException` is thrown.
- [ ] **Room Capacity Gate 2**:
    - Schedule a section with `maxCapacity = 50` into a room with `capacity = 30`; assert `IllegalArgumentException` is thrown with clear error detail.
- [ ] **Transferee GPA Integrity**:
    - Credit 2 courses (3 units with grade 1.25, 3 units with grade 1.75); assert student cumulative GPA updates to `1.50` rather than remaining null/stale.
- [ ] **IDOR Protection & Fallback Removal**:
    - Send an overload approval request with an unauthenticated context; assert `AccessDeniedException` (HTTP 403), never attributing approval to User 1.
- [ ] **Student Route Guarding**:
    - Navigate to `/dashboard/grades` with `ROLE_STUDENT`; assert redirect to `/dashboard`.
- [ ] **Password Reset Hash Verification**:
    - Request password reset; verify raw token is sent via email, but token persisted in `password_reset_tokens` is a 64-character SHA-256 hex string. Verify logs contain no plaintext reset URLs.

---

_Generated by Antigravity Systems Architect & Code Remediation Specialist._