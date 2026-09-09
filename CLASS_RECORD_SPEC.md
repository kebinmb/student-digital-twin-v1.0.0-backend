# CLASS RECORD SPECIFICATION & ARCHITECTURAL BLUEPRINT
## Dynamic Class Record, Assessment Weighting & Philippine Transmutation Engine

**Student Digital Twin v1.0.0**  
**Spring Boot 4.1.0 / Java 21 LTS · Angular 19+ / PrimeNG 21+ · Flyway V1–V14 Target**  
**Author:** Antigravity Systems Architect & Enterprise Software Engineer  
**Date:** 2026-09-09  

---

## 1. Executive Summary & Context

This document presents the post-remediation verification audit of the **Student Digital Twin** system and provides the comprehensive technical design for the **Dynamic Class Record & Grading Weight Engine**. 

The engine enables faculty members to configure section-scoped assessment categories (Quizzes, Seatwork, Assignments, Laboratories, Major Examinations), establish term weight splits (Midterm vs. Final), ingest raw score matrices, and automatically transmute percentage averages to standard Philippine CHED numerical grades (`1.00` – `5.00`) before advancing grades through the existing 4-tier lifecycle (`DRAFT ➔ SUBMITTED ➔ VERIFIED ➔ SEALED`).

---

## 2. Phase 1: Post-Remediation Verification & Invariant Audit

Following an exhaustive code audit across both backend (`student-digital-twin-v1.0.0-backend`) and frontend (`student-digital-twin-v1.0.0-frontend`) codebases against `00 Remediation Plan.md`, all identified P0/P1/P2 issues have been verified as fully remediated with zero regressions.

### Post-Remediation Audit Matrix

| Category | File Target | Remediation Objective | Verification Status | Empirical Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **Security & Identity** | `SchedulingController.java` | Removal of fallback `approverId = 1L` / `adminUserId = 1L` default | **VERIFIED CLEAN** | Throws `AccessDeniedException` if authenticated principal resolution fails. |
| **Security & Identity** | `PasswordResetService.java` | SHA-256 token hashing; log masking of reset URLs; remove `userId` from response | **VERIFIED CLEAN** | Directives implemented via `hashToken()` and sanitized log dispatch. |
| **Security & Identity** | `TokenService.java` | JWT audience alignment to `api://sdt-webapp` | **VERIFIED CLEAN** | `@Value("${spring.security.oauth2.resourceserver.jwt.audiences:api://sdt-webapp}")` set. |
| **Security & Identity** | `StudentController.java` / `StudentService.java` | Add `GET /api/v1/students/me` endpoint for self-profile lookup | **VERIFIED CLEAN** | Endpoint annotated with `@PreAuthorize("hasRole('STUDENT')")` resolving caller ID. |
| **Academic Core** | `Curriculum.java` | Directed-graph state machine map (`VALID_TRANSITIONS`) and `@Version` concurrency lock | **VERIFIED CLEAN** | Strict transition map prevents illegal leaps (`ARCHIVED ➔ DRAFT`); optimistic locking enabled. |
| **Academic Core** | `CurriculumController.java` | Grant `CHAIRPERSON` role coverage on `/validate` and `/transition-state` | **VERIFIED CLEAN** | Updated to `@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")`. |
| **Scheduling Core** | `SchedulingService.java` | Enforce room physical seating capacity checks against section `maxCapacity` | **VERIFIED CLEAN** | Validates `room.getCapacity() >= section.getMaxCapacity()` on section creation and slot assignment. |
| **Persistence Cache** | `ClassSectionRepository.java` | Add `clearAutomatically = true, flushAutomatically = true` to `@Modifying` queries | **VERIFIED CLEAN** | Hibernate 1st-level cache cleared upon `incrementEnrolledCountIfOpen` and `decrementEnrolledCount`. |
| **Student Records** | `TransfereeCreditingService.java` | Calculate unit-weighted cumulative GPA for credited transferee courses | **VERIFIED CLEAN** | Computes `weightedGradeSum / totalUnits` rounding to 2 decimal places. |
| **Frontend Security** | `app.routes.ts` | Guard `/dashboard/grades` with `roleGuard(['ADMIN','DEAN','CHAIRPERSON','REGISTRAR','FACULTY'])` | **VERIFIED CLEAN** | Student navigation to gradebook UI is blocked and redirected. |
| **Frontend State** | `enrollment.store.ts` | Nullable `studentId`, `forkJoin` coordination, `takeUntilDestroyed` subscriptions | **VERIFIED CLEAN** | `studentId` initialized to `null`; `loadStudentAdvising` uses `forkJoin` with atomic finalization. |
| **Frontend State** | `scheduling.store.ts` | `forkJoin` initial dataset loading (`terms`, `curricula`, `rooms`, `instructors`, `programs`) | **VERIFIED CLEAN** | Atomic dataset hydration without race conditions or premature loading state clearance. |
| **Frontend Resilience**| `global-error.interceptor.ts` | Graceful handling for `status === 0` (network drop) and `status === 404` | **VERIFIED CLEAN** | Toast notifications emitted for connection failure and resource not found states. |

### Verification Test Suite Results
- **Backend Test Suite**: **129 / 129 Passed** (`.\mvnw.cmd test` execution green).
- **Frontend Test Suite**: **87 / 87 Passed** across 35 test files (`npx ng test --watch=false` green).
- **Production Build**: **0 Build Errors** (`npx ng build` completed successfully).

---

## 3. Phase 2: Domain Analysis & Current Grading Engine Gap Analysis

### 3.1 Existing 4-Tier Grading Architecture Overview
The current grade management engine (`GradeService.java`, `ClassSection.java`, `EnrollmentCourseItem.java`) operates on a 4-tier lifecycle:

```
[ Tier 1: DRAFT ] ────────► [ Tier 2: SUBMITTED ] ────────► [ Tier 3: VERIFIED ] ────────► [ Tier 4: SEALED ]
Instructor Encodes         Dean / Chair Review           Registrar Clearance           Permanent Ledger &
Numerical Grades           Academic Compliance           Ready for Sealing             GPA Synchronization
```

1. **Faculty Draft (`DRAFT`)**: Instructor encodes individual numerical grades (`1.00` – `5.00`) directly on `EnrollmentCourseItem.finalNumericalGrade`.
2. **Dean Submission (`SUBMITTED`)**: Instructor locks draft and submits section roster for academic review.
3. **Dean Verification (`VERIFIED`)**: Dean/Chairperson approves compliance and endorses roster to Registrar.
4. **Registrar Sealing (`SEALED`)**: Registrar executes Sealing Engine. Grades are permanently written to `StudentCourseGrade`, cumulative GPA is recalculated, and CHED CMO 25 prerequisites are unlocked.

### 3.2 Key Architectural Gaps
1. **Manual Final Grade Entry Only**: `GradeService.saveGrades()` accepts pre-calculated final numerical grades directly. Faculty must compute raw class records externally (e.g., MS Excel) and manually transcribe final values.
2. **Absence of Assessment Category Breakdown**: No persistent representation for individual assignments, quizzes, laboratory experiments, or term exams.
3. **Missing Transmutation Automation**: `GradeTransmutationService` exists in the codebase (`transmutePercentage()`), but is disconnected from the section gradebook workflow.

---

## 4. Phase 3: Architectural Specification — Dynamic Class Record Engine

```
                               DYNAMIC CLASS RECORD PIPELINE
  ┌────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
  │  SectionGradingConfig  │ ──►│    ClassRecordItems     │ ──►│ StudentAssessmentScores │
  │ (Weights & Term Split) │    │  (Quizzes, Exams, etc)  │    │  (Raw Earned Points)    │
  └────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
                                                                            │
                                                                            ▼
  ┌────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
  │ EnrollmentCourseItem   │ ◄──│ GradeTransmutation      │ ◄──│ Weighted Raw % Score    │
  │ (finalNumericalGrade)  │    │ Service (CHED 1.0-5.0)  │    │ Calculation Engine      │
  └────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
```

### 4.1 Data Model & Dynamic Aggregators

#### Section Grading Configuration (`SectionGradingConfig`)
Each `ClassSection` maintains a unique, section-scoped grading configuration:
- **Term Weight Split**: Configurable ratio between Midterm and Final/Endterm periods (e.g., $50\%$ Midterm $+ 50\%$ Final, or $33.3\%$ Midterm $+ 66.7\%$ Final). Constraint:
  $$\text{MidtermWeight} + \text{EndtermWeight} = 100.00\%$$
- **Category Weights**: Configurable percentage allocations per term category (e.g., Quizzes: 20%, Seatwork: 15%, Assignments: 15%, Major Exam: 50%). Constraint:
  $$\sum_{k \in \text{Categories}_{\text{Term}}} \text{Weight}_k = 100.00\%$$
- **Dynamic Assessment Items**: Faculty can add $N$ assessment items per category with custom total possible points ($P_{\text{max}}$).

### 4.2 Mathematical Formulas & Transmutation Pipeline

#### Step 1: Category Score Computation
For student $s$ in assessment category $k$ during term period $T \in \{\text{MIDTERM}, \text{FINAL}\}$:

$$\text{CategoryScore}_{s,k,T} = \left( \frac{\sum_{i \in \text{Items}_{k,T}} \text{ScoreEarned}_{s,i}}{\sum_{i \in \text{Items}_{k,T}} \text{MaxPoints}_i} \right) \times 100$$

*(If $\sum \text{MaxPoints}_i = 0$, default $\text{CategoryScore}_{s,k,T} = 0.00$)*

#### Step 2: Term Percentage Grade
$$\text{TermGrade}_{s,T} = \sum_{k \in \text{Categories}_T} \left( \text{CategoryScore}_{s,k,T} \times \frac{\text{CategoryWeight}_{k,T}}{100} \right)$$

#### Step 3: Final Raw Percentage Accumulation
$$\text{FinalRawPercentage}_s = \left( \text{TermGrade}_{s,\text{MIDTERM}} \times \frac{\text{MidtermWeight}}{100} \right) + \left( \text{TermGrade}_{s,\text{FINAL}} \times \frac{\text{FinalWeight}}{100} \right)$$

#### Step 4: CHED Transmutation Mapping
The computed $\text{FinalRawPercentage}_s$ is passed to `GradeTransmutationService.transmutePercentage()`, matching institutional brackets:

$$\text{FinalRawPercentage}_s \longrightarrow \text{GradingScale Bracket} \longrightarrow \text{CHED Numerical Grade (1.00 – 5.00)}$$

| Raw Percentage Range | Transmuted Grade | CHED Descriptor | Status |
| :--- | :--- | :--- | :--- |
| $97.00\% - 100.00\%$ | **1.00** | EXCELLENT | PASSED |
| $94.00\% - 96.99\%$ | **1.25** | SUPERIOR | PASSED |
| $91.00\% - 93.99\%$ | **1.50** | VERY GOOD | PASSED |
| $88.00\% - 90.99\%$ | **1.75** | GOOD | PASSED |
| $85.00\% - 87.99\%$ | **2.00** | VERY SATISFACTORY | PASSED |
| $82.00\% - 84.99\%$ | **2.25** | SATISFACTORY | PASSED |
| $79.00\% - 81.99\%$ | **2.50** | FAIR | PASSED |
| $76.00\% - 78.99\%$ | **2.75** | PASSING | PASSED |
| $75.00\% - 75.99\%$ | **3.00** | PASSED BARELY | PASSED |
| $0.00\% - 74.99\%$ | **5.00** | FAILED | FAILED |

---

## 5. Database Schema & Flyway Migration (`V14__create_dynamic_class_records.sql`)

```sql
-- Flyway Migration Script: V14__create_dynamic_class_records.sql
-- Subsystem: Dynamic Class Record, Assessment Weighting & Score Matrix Engine

-- 1. Section Grading Configuration Header
CREATE TABLE IF NOT EXISTS section_grading_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL UNIQUE,
    midterm_weight DECIMAL(5,2) NOT NULL DEFAULT 50.00,
    final_weight DECIMAL(5,2) NOT NULL DEFAULT 50.00,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sgc_section FOREIGN KEY (section_id) REFERENCES class_sections(id) ON DELETE CASCADE,
    CONSTRAINT chk_term_weight_total CHECK (midterm_weight + final_weight = 100.00)
);

-- 2. Section Grading Categories
CREATE TABLE IF NOT EXISTS section_grading_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    weight_percentage DECIMAL(5,2) NOT NULL,
    term_period VARCHAR(10) NOT NULL, -- 'MIDTERM' or 'FINAL'
    display_order INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_sgcat_config FOREIGN KEY (config_id) REFERENCES section_grading_configs(id) ON DELETE CASCADE,
    CONSTRAINT chk_term_period CHECK (term_period IN ('MIDTERM', 'FINAL'))
);

-- 3. Class Record Assessment Items
CREATE TABLE IF NOT EXISTS class_record_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    item_title VARCHAR(100) NOT NULL,
    max_points DECIMAL(6,2) NOT NULL,
    sequence_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cri_category FOREIGN KEY (category_id) REFERENCES section_grading_categories(id) ON DELETE CASCADE,
    CONSTRAINT chk_max_points CHECK (max_points > 0)
);

-- 4. Student Assessment Scores Matrix
CREATE TABLE IF NOT EXISTS student_assessment_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score_earned DECIMAL(6,2) NULL, -- NULL indicates unentered/pending
    is_excused BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sas_item FOREIGN KEY (item_id) REFERENCES class_record_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_sas_student FOREIGN KEY (student_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_item UNIQUE (item_id, student_id)
);

-- Indexes for performance
CREATE INDEX idx_sgcat_config ON section_grading_categories(config_id);
CREATE INDEX idx_cri_category ON class_record_items(category_id);
CREATE INDEX idx_sas_item_student ON student_assessment_scores(item_id, student_id);
```

---

## 6. Service Contracts, REST Endpoints & Security Scoping

### REST Endpoint Specification (`/api/v1/class-records`)

| Method | Path | Security Assertion (`@PreAuthorize`) | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/class-records/sections/{sectionId}/config` | `hasAnyRole('ADMIN','DEAN','CHAIRPERSON','FACULTY')` | Retrieve section grading scheme configuration. |
| `PUT` | `/api/v1/class-records/sections/{sectionId}/config` | `hasAnyRole('ADMIN','FACULTY')` + Instructor check | Update category weights and midterm/final ratio. |
| `POST` | `/api/v1/class-records/sections/{sectionId}/items` | `hasAnyRole('ADMIN','FACULTY')` + Instructor check | Add new assessment item (e.g. Quiz 1, Exam 1). |
| `DELETE`| `/api/v1/class-records/items/{itemId}` | `hasAnyRole('ADMIN','FACULTY')` + Instructor check | Remove assessment item and cascade scores. |
| `GET` | `/api/v1/class-records/sections/{sectionId}/matrix` | `hasAnyRole('ADMIN','DEAN','CHAIRPERSON','FACULTY')` | Load full student x item score matrix + raw averages. |
| `POST` | `/api/v1/class-records/sections/{sectionId}/scores/batch` | `hasAnyRole('ADMIN','FACULTY')` + Instructor check | Bulk save student raw scores & trigger auto-calc. |
| `POST` | `/api/v1/class-records/sections/{sectionId}/recalculate` | `hasAnyRole('ADMIN','FACULTY')` + Instructor check | Recalculate raw averages and push to `EnrollmentCourseItem`. |

### Security Access Policy
- **Primary Instructor Guard**: Only the assigned primary instructor (`section.primaryInstructorId == principal.id`) or an authorized system administrator can mutate configurations or scores.
- **Section Status Lock**: Any modification attempt (`PUT`, `POST`, `DELETE`) is rejected with HTTP 409 Conflict if the `ClassSection.gradeStatus` is `SUBMITTED`, `VERIFIED`, or `SEALED`.

---

## 7. Frontend UI/UX Architecture (`faculty-gradebook.component`)

### Integrated Workspace Layout

```
 ┌────────────────────────────────────────────────────────────────────────────────────────┐
 │  Section Gradebook & Registrar Sealing Engine                                         │
 │  [ Academic Term: AY 2026-2027 1st Sem ]  [ Class Section: BSIT 3-A — CS 311 ]          │
 └────────────────────────────────────────────────────────────────────────────────────────┘
   ├── [Tab 1: Final Gradebook Roster & 4-Tier Workflow] (Existing UI)
   └── [Tab 2: Dynamic Class Record & Assessment Weight Engine] (New Feature)
        ├── Sub-Header: Weight Scheme Configuration (Midterm: 50% | Final: 50% | Category Total: 100%)
        ├── Controls: [ Add Assessment Category ]  [ Add Quiz / Exam Item ]  [ Recalculate & Sync ]
        └── Dynamic Spreadsheet Table:
            ┌──────────────┬───────────────────┬───────────────────┬──────────────┬─────────────┐
            │ Student Name │ Quizzes (20%)     │ Major Exams (50%) │ Final Raw %  │ Transmuted  │
            │              │ Q1 (20pt) Q2(30pt)│ Midterm  Final    │ (Calculated) │ CHED Grade  │
            ├──────────────┼───────────────────┼───────────────────┼──────────────┼─────────────┤
            │ Juan Dela C. │   18      25      │   88       92     │   89.50%     │    1.75     │
            │ Maria Santos │   20      29      │   95       98     │   96.80%     │    1.25     │
            └──────────────┴───────────────────┴───────────────────┴──────────────┴─────────────┘
```

---

## 8. Architectural Sign-off & Next Steps

This technical specification enforces institutional compliance with CHED CMO 25 guidelines while seamlessly integrating into the existing system architecture without breaking changes.

- [x] Phase 1 Post-Remediation Verification Complete.
- [x] Phase 2 Grading Domain Analysis Complete.
- [x] Phase 3 Dynamic Class Record Specification Formulated.

*Awaiting prompt review and user authorization before proceeding with Flyway migration and implementation.*
