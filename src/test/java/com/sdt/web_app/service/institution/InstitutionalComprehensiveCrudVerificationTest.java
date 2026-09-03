package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.AcademicYearDtos.*;
import com.sdt.web_app.dto.institution.CampusDtos.*;
import com.sdt.web_app.dto.institution.CiloPiloMappingDtos.*;
import com.sdt.web_app.dto.institution.CourseDtos.*;
import com.sdt.web_app.dto.institution.CourseOutcomeDtos.*;
import com.sdt.web_app.dto.institution.CoursePrerequisiteDtos.*;
import com.sdt.web_app.dto.institution.CurriculumCourseDtos.*;
import com.sdt.web_app.dto.institution.CurriculumDtos.*;
import com.sdt.web_app.dto.institution.DepartmentDtos.*;
import com.sdt.web_app.dto.institution.ProgramDtos.*;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.institution.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InstitutionalComprehensiveCrudVerificationTest {

    @Autowired private AcademicYearService academicYearService;
    @Autowired private TermService termService;
    @Autowired private TermLifecycleService termLifecycleService;
    @Autowired private CampusService campusService;
    @Autowired private DepartmentService departmentService;
    @Autowired private ProgramService programService;
    @Autowired private CourseService courseService;
    @Autowired private CourseOutcomeService courseOutcomeService;
    @Autowired private CoursePrerequisiteService prerequisiteService;
    @Autowired private CiloPiloMappingService mappingService;
    @Autowired private CurriculumService curriculumService;
    @Autowired private GradingScaleService gradingScaleService;
    @Autowired private GradeTransmutationService transmutationService;
    @Autowired private FinancialStructureService financialService;

    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private TermRepository termRepository;
    @Autowired private CampusRepository campusRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private ProgramOutcomeRepository programOutcomeRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CurriculumRepository curriculumRepository;
    @Autowired private CurriculumCourseRepository curriculumCourseRepository;

    // =========================================================================
    // 1. ACADEMIC PERIODS & CALENDAR (AcademicYear, Term)
    // =========================================================================
    @Nested
    @DisplayName("1. Academic Periods & Calendar (AcademicYear, Term)")
    class AcademicPeriodsTests {

        @Test
        @DisplayName("CREATE & READ: AcademicYear persists with unique code, validates dates, and atomicity on isCurrent")
        void testAcademicYear_CreateAndRead() {
            // Reject when endDate <= startDate
            assertThatThrownBy(() -> academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                    "AY-INVALID",
                    LocalDate.of(2027, 8, 1),
                    LocalDate.of(2027, 5, 1), // Invalid: before start
                    false
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("End date must be after start date");

            // Persist AY 1
            AcademicYearResponse ay1 = academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                    "AY-2035-2036",
                    LocalDate.of(2035, 8, 1),
                    LocalDate.of(2036, 6, 30),
                    true
            ));
            assertThat(ay1.id()).isNotNull();
            assertThat(ay1.isCurrent()).isTrue();

            // Persist AY 2 with isCurrent = true (should demote AY 1)
            AcademicYearResponse ay2 = academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                    "AY-2036-2037",
                    LocalDate.of(2036, 8, 1),
                    LocalDate.of(2037, 6, 30),
                    true
            ));
            assertThat(ay2.isCurrent()).isTrue();
            assertThat(academicYearService.getAcademicYearById(ay1.id()).isCurrent()).isFalse();

            // Duplicate code rejection
            assertThatThrownBy(() -> academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                    "AY-2036-2037",
                    LocalDate.of(2036, 8, 1),
                    LocalDate.of(2037, 6, 30),
                    false
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("already exists");

            // Read all ordered by start date desc
            List<AcademicYearResponse> all = academicYearService.getAllAcademicYears();
            assertThat(all).isNotEmpty();
            assertThat(all.getFirst().startDate()).isAfterOrEqualTo(all.getLast().startDate());
        }

        @Test
        @DisplayName("UPDATE: AcademicYear updates dates cleanly")
        void testAcademicYear_Update() {
            AcademicYearResponse ay = academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                    "AY-UPDATE-TEST",
                    LocalDate.of(2040, 8, 1),
                    LocalDate.of(2041, 6, 30),
                    false
            ));

            LocalDate newStart = LocalDate.of(2040, 9, 1);
            LocalDate newEnd = LocalDate.of(2041, 7, 15);
            AcademicYearResponse updated = academicYearService.updateAcademicYear(ay.id(),
                    new UpdateAcademicYearRequest(newStart, newEnd));

            assertThat(updated.startDate()).isEqualTo(newStart);
            assertThat(updated.endDate()).isEqualTo(newEnd);
        }

        @Test
        @DisplayName("DELETE: AcademicYear deletion guarded when referenced by child terms")
        void testAcademicYear_DeleteGuard() {
            AcademicYearResponse ay = academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                    "AY-DEL-GUARD",
                    LocalDate.of(2042, 8, 1),
                    LocalDate.of(2043, 6, 30),
                    false
            ));

            // Create child term
            termService.createTerm(ay.id(), TermType.FIRST_SEM, LocalDate.of(2042, 8, 1), LocalDate.of(2042, 12, 20));

            // Attempt deletion -> guarded!
            assertThatThrownBy(() -> academicYearService.deleteAcademicYear(ay.id()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete academic year referenced by academic terms");
        }

        @Test
        @DisplayName("Term CRUD: Precondition verification, schedule mutation, lifecycle activation")
        void testTerm_CrudAndLifecycle() {
            AcademicYearResponse ay = academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                    "AY-TERM-CRUD",
                    LocalDate.of(2045, 8, 1),
                    LocalDate.of(2046, 6, 30),
                    false
            ));

            // Precondition: Invalid academic year ID
            assertThatThrownBy(() -> termService.createTerm(999999L, TermType.FIRST_SEM, LocalDate.of(2045, 8, 1), LocalDate.of(2045, 12, 20)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Academic year not found");

            // Valid Term Creation
            Term term1 = termService.createTerm(ay.id(), TermType.FIRST_SEM, LocalDate.of(2045, 8, 1), LocalDate.of(2045, 12, 20));
            assertThat(term1.getId()).isNotNull();
            assertThat(term1.getTermType()).isEqualTo(TermType.FIRST_SEM);

            // Duplicate (academic_year_id, term_type) rejection
            assertThatThrownBy(() -> termService.createTerm(ay.id(), TermType.FIRST_SEM, LocalDate.of(2045, 8, 1), LocalDate.of(2045, 12, 20)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists for academic year");

            // Read terms by academic year
            List<Term> terms = termService.getTermsByAcademicYear(ay.id());
            assertThat(terms).hasSize(1);

            // Update term schedule
            LocalDate updatedEnd = LocalDate.of(2045, 12, 31);
            Term updatedTerm = termService.updateTermSchedule(term1.getId(), LocalDate.of(2045, 8, 15), updatedEnd);
            assertThat(updatedTerm.getEndDate()).isEqualTo(updatedEnd);

            // Lifecycle activation
            Term activated = termLifecycleService.activateTerm(term1.getId());
            assertThat(activated.isActive()).isTrue();

            // Deletion blocked for active term
            assertThatThrownBy(() -> termService.deleteTerm(term1.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete an active operational academic term");
        }
    }

    // =========================================================================
    // 2. ORGANIZATIONAL HIERARCHY (Campus, Department, Program)
    // =========================================================================
    @Nested
    @DisplayName("2. Organizational Hierarchy (Campus, Department, Program)")
    class OrganizationalHierarchyTests {

        @Test
        @DisplayName("Campus CRUD: Unique code, single main campus exclusivity, and active status toggle")
        void testCampus_Crud() {
            CampusResponse c1 = campusService.createCampus(new CreateCampusRequest(
                    "CAMPUS-A",
                    "Campus Alpha Main",
                    "06001",
                    "Address 1",
                    "REGION VI",
                    "12345",
                    "alpha@chmsu.edu.ph",
                    true
            ));
            assertThat(c1.isMain()).isTrue();

            // Create Campus B as main -> Demotes Campus A
            CampusResponse c2 = campusService.createCampus(new CreateCampusRequest(
                    "CAMPUS-B",
                    "Campus Beta",
                    "06002",
                    "Address 2",
                    "REGION VI",
                    "67890",
                    "beta@chmsu.edu.ph",
                    true
            ));
            assertThat(c2.isMain()).isTrue();
            assertThat(campusService.getCampusById(c1.id()).isMain()).isFalse();

            // Update details
            CampusResponse updated = campusService.updateCampus(c1.id(), new UpdateCampusRequest(
                    "Campus Alpha Renamed",
                    "06001-REV",
                    "New Address",
                    "12345-REV",
                    "alpha.new@chmsu.edu.ph"
            ));
            assertThat(updated.name()).isEqualTo("Campus Alpha Renamed");

            // Toggle active status
            CampusResponse deactivated = campusService.toggleCampusActive(c1.id(), false);
            assertThat(deactivated.isActive()).isFalse();
        }

        @Test
        @DisplayName("Department CRUD: Campus prerequisite, cyclic self-parenting guard, and deletion guard")
        void testDepartment_CrudAndGuards() {
            CampusResponse campus = campusService.createCampus(new CreateCampusRequest(
                    "CAMPUS-DEPT-TEST",
                    "Campus For Dept",
                    null, null, "REGION VI", null, null, false
            ));

            // Precondition check: Invalid campus ID
            assertThatThrownBy(() -> departmentService.createDepartment(new CreateDepartmentRequest(
                    999999L, "CCS-FAIL", "College of Fail", DepartmentType.COLLEGE, null, null
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Campus not found");

            // Create College
            DepartmentResponse college = departmentService.createDepartment(new CreateDepartmentRequest(
                    campus.id(), "COLLEGE-1", "College of Engineering", DepartmentType.COLLEGE, null, null
            ));
            assertThat(college.id()).isNotNull();

            // Create Child Department under College
            DepartmentResponse subDept = departmentService.createDepartment(new CreateDepartmentRequest(
                    campus.id(), "DEPT-CE", "Civil Engineering Dept", DepartmentType.DEPARTMENT, college.id(), null
            ));
            assertThat(subDept.parentDepartmentId()).isEqualTo(college.id());

            // Prevent cyclic self-parenting on update
            assertThatThrownBy(() -> departmentService.updateDepartment(college.id(), new UpdateDepartmentRequest(
                    "College of Engineering", DepartmentType.COLLEGE, college.id(), null
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("cannot be its own parent");

            // Deletion guard: Campus cannot be deleted if departments exist
            assertThatThrownBy(() -> campusService.deleteCampus(campus.id()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("contains existing departments");

            // Deletion guard: College cannot be deleted if sub-departments exist
            assertThatThrownBy(() -> departmentService.deleteDepartment(college.id()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has child sub-departments");
        }

        @Test
        @DisplayName("Program CRUD: Department prerequisite and curriculum deletion guard")
        void testProgram_CrudAndGuards() {
            CampusResponse campus = campusService.createCampus(new CreateCampusRequest(
                    "CAMPUS-PROG", "Campus Prog", null, null, "REGION VI", null, null, false
            ));
            DepartmentResponse dept = departmentService.createDepartment(new CreateDepartmentRequest(
                    campus.id(), "DEPT-PROG", "Dept of Computing", DepartmentType.COLLEGE, null, null
            ));

            // Precondition: Invalid department ID
            assertThatThrownBy(() -> programService.createProgram(
                    999999L, "BSIT-FAIL", "Fail Program", null, "UNDERGRADUATE", 140
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Department not found");

            // Valid Program creation
            ProgramResponse prog = programService.createProgram(
                    dept.id(), "BSIT-VERIF", "BS in Information Technology", "Network", "UNDERGRADUATE", 146
            );
            assertThat(prog.id()).isNotNull();

            // Update Program details
            ProgramResponse updated = programService.updateProgram(
                    prog.id(), "BSIT Renamed", "Software", "CMO 25 s. 2015", "GR-2026", 150
            );
            assertThat(updated.name()).isEqualTo("BSIT Renamed");

            // Program Outcome CRUD
            ProgramOutcomeResponse po = programService.createProgramOutcome(prog.id(), "PILO-A", "Demonstrate mastery of algorithms.");
            assertThat(po.id()).isNotNull();

            // Program deletion guard when curriculum references it
            curriculumService.createCurriculum(new CreateCurriculumRequest(
                    prog.id(), "CURR-PROG-GUARD", "Curriculum Guard", "2026-2027"
            ));

            assertThatThrownBy(() -> programService.deleteProgram(prog.id()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("referenced by existing curricula");
        }
    }

    // =========================================================================
    // 3. COURSE CATALOG & OUTCOMES (Course, Outcome, Prerequisite, CILO-PILO)
    // =========================================================================
    @Nested
    @DisplayName("3. Course Catalog & Outcomes (Course, Outcome, Prerequisite, CILO-PILO)")
    class CourseCatalogAndOutcomesTests {

        @Test
        @DisplayName("Course CRUD: Auto-calculation of creditUnits, category persistence, and deletion guard")
        void testCourse_CrudAndGuards() {
            // Create Course with default category (PROFESSIONAL_MAJOR)
            CourseResponse course = courseService.createCourse(new CreateCourseRequest(
                    "CS-100-VERIF",
                    "Discrete Structures",
                    new BigDecimal("2.00"),
                    new BigDecimal("1.00"),
                    2,
                    3,
                    "Discrete mathematics foundations."
            ));
            assertThat(course.creditUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(course.category()).isEqualTo("PROFESSIONAL_MAJOR");

            // Update Course with explicit Category (ELECTIVE)
            CourseResponse updated = courseService.updateCourse(course.id(), new UpdateCourseRequest(
                    "Discrete Mathematics & Graph Theory",
                    new BigDecimal("3.00"),
                    new BigDecimal("0.00"),
                    3,
                    0,
                    "ELECTIVE",
                    "Updated description."
            ));
            assertThat(updated.creditUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
            assertThat(updated.category()).isEqualTo("ELECTIVE");

            // Create Course with specific category (CAPSTONE)
            CourseResponse capstone = courseService.createCourse(new CreateCourseRequest(
                    "IT-413-CAP",
                    "Capstone Project 1",
                    new BigDecimal("3.00"),
                    BigDecimal.ZERO,
                    3,
                    0,
                    "CAPSTONE",
                    "Capstone proposal."
            ));
            assertThat(capstone.category()).isEqualTo("CAPSTONE");

            // Search with pagination
            var page = courseService.searchCourses("Discrete", PageRequest.of(0, 10));
            assertThat(page.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("CourseOutcome (CILO) CRUD & Deletion guard with CILO-PILO mapping")
        void testCourseOutcome_CrudAndGuards() {
            CourseResponse course = courseService.createCourse(new CreateCourseRequest(
                    "CS-CILO-TEST", "Course for CILO", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));

            // Precondition: Non-existent course
            assertThatThrownBy(() -> courseOutcomeService.createCourseOutcome(999999L,
                    new CreateCourseOutcomeRequest("CILO-1", "Test Outcome", "Applying")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Course not found");

            // Create CILO
            CourseOutcomeResponse cilo = courseOutcomeService.createCourseOutcome(course.id(),
                    new CreateCourseOutcomeRequest("CILO-1", "Understand fundamentals", "Remembering"));
            assertThat(cilo.id()).isNotNull();

            // Duplicate code rejection
            assertThatThrownBy(() -> courseOutcomeService.createCourseOutcome(course.id(),
                    new CreateCourseOutcomeRequest("CILO-1", "Duplicate Code", "Applying")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists for course");

            // Update CILO
            CourseOutcomeResponse updated = courseOutcomeService.updateCourseOutcome(cilo.id(),
                    new UpdateCourseOutcomeRequest("Understand fundamentals deeply", "Understanding"));
            assertThat(updated.description()).isEqualTo("Understand fundamentals deeply");
            assertThat(updated.bloomsLevel()).isEqualTo("Understanding");
        }

        @Test
        @DisplayName("CoursePrerequisite: Self-prerequisite rejection and DAG circular dependency detection")
        void testCoursePrerequisite_ValidationsAndCycleCheck() {
            CourseResponse c1 = courseService.createCourse(new CreateCourseRequest(
                    "CS-PREREQ-1", "Intro to Programming", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));
            CourseResponse c2 = courseService.createCourse(new CreateCourseRequest(
                    "CS-PREREQ-2", "Object-Oriented Programming", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));
            CourseResponse c3 = courseService.createCourse(new CreateCourseRequest(
                    "CS-PREREQ-3", "Data Structures & Algorithms", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));

            // Self-prerequisite guard: Course cannot require itself
            assertThatThrownBy(() -> prerequisiteService.createPrerequisite(new CreateCoursePrerequisiteRequest(
                    c1.id(), c1.id(), "HARD", "3.00"
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("cannot be its own prerequisite");

            // Chain: C2 requires C1; C3 requires C2
            prerequisiteService.createPrerequisite(new CreateCoursePrerequisiteRequest(c2.id(), c1.id(), "HARD", "3.00"));
            prerequisiteService.createPrerequisite(new CreateCoursePrerequisiteRequest(c3.id(), c2.id(), "HARD", "3.00"));

            // Circular Dependency Loop: C1 requires C3 -> introduces cycle (C1 -> C3 -> C2 -> C1)!
            assertThatThrownBy(() -> prerequisiteService.createPrerequisite(new CreateCoursePrerequisiteRequest(
                    c1.id(), c3.id(), "HARD", "3.00"
            ))).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("circular dependency");
        }

        @Test
        @DisplayName("CILO-PILO Matrix: Existence preconditions, mapping type validation, and upsert")
        void testCiloPiloMapping_PreconditionsAndUpsert() {
            CampusResponse campus = campusService.createCampus(new CreateCampusRequest(
                    "CAMPUS-MATRIX", "Campus Matrix", null, null, "REGION VI", null, null, false
            ));
            DepartmentResponse dept = departmentService.createDepartment(new CreateDepartmentRequest(
                    campus.id(), "DEPT-MATRIX", "Dept Matrix", DepartmentType.COLLEGE, null, null
            ));
            ProgramResponse prog = programService.createProgram(
                    dept.id(), "PROG-MATRIX", "Program Matrix", null, "UNDERGRADUATE", 140
            );
            ProgramOutcomeResponse pilo = programService.createProgramOutcome(prog.id(), "PILO-M1", "Outcome 1");

            CourseResponse course = courseService.createCourse(new CreateCourseRequest(
                    "CS-MATRIX-1", "Course Matrix", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));
            CourseOutcomeResponse cilo = courseOutcomeService.createCourseOutcome(course.id(),
                    new CreateCourseOutcomeRequest("CILO-M1", "Course Outcome 1", "Applying"));

            // Precondition: Invalid CILO ID
            assertThatThrownBy(() -> mappingService.createOrUpdateMapping(new CreateCiloPiloMappingRequest(
                    999999L, pilo.id(), "I"
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Course outcome not found");

            // Precondition: Invalid PILO ID
            assertThatThrownBy(() -> mappingService.createOrUpdateMapping(new CreateCiloPiloMappingRequest(
                    cilo.id(), 999999L, "I"
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Program outcome not found");

            // Invalid mapping type (not I, E, or D)
            assertThatThrownBy(() -> mappingService.createOrUpdateMapping(new CreateCiloPiloMappingRequest(
                    cilo.id(), pilo.id(), "X"
            ))).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Mapping type must be 'I', 'E', or 'D'");

            // Valid initial mapping
            CiloPiloMappingResponse m1 = mappingService.createOrUpdateMapping(new CreateCiloPiloMappingRequest(
                    cilo.id(), pilo.id(), "I"
            ));
            assertThat(m1.mappingType()).isEqualTo("I");

            // Upsert (update to 'D')
            CiloPiloMappingResponse m2 = mappingService.createOrUpdateMapping(new CreateCiloPiloMappingRequest(
                    cilo.id(), pilo.id(), "D"
            ));
            assertThat(m2.id()).isEqualTo(m1.id());
            assertThat(m2.mappingType()).isEqualTo("D");

            // Delete guard on CILO: Cannot delete CILO when mapped in matrix
            assertThatThrownBy(() -> courseOutcomeService.deleteCourseOutcome(cilo.id()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("linked to program outcomes in CILO-PILO matrix");

            // Delete mapping cleanly
            mappingService.deleteMapping(m2.id());
            // Now CILO can be deleted!
            courseOutcomeService.deleteCourseOutcome(cilo.id());
        }
    }

    // =========================================================================
    // 4. CURRICULUM STRUCTURE (Curriculum, CurriculumCourse)
    // =========================================================================
    @Nested
    @DisplayName("4. Curriculum Structure (Curriculum, CurriculumCourse)")
    class CurriculumStructureTests {

        @Test
        @DisplayName("Curriculum & Course Assignment: Immutability on locked curricula & sequence order")
        void testCurriculum_ImmutabilityAndAssignment() {
            CampusResponse campus = campusService.createCampus(new CreateCampusRequest(
                    "CAMPUS-CURR-TEST", "Campus Curr", null, null, "REGION VI", null, null, false
            ));
            DepartmentResponse dept = departmentService.createDepartment(new CreateDepartmentRequest(
                    campus.id(), "DEPT-CURR-TEST", "Dept Curr", DepartmentType.COLLEGE, null, null
            ));
            ProgramResponse prog = programService.createProgram(
                    dept.id(), "BSIT-CURR-SPEC", "BSIT Spec", null, "UNDERGRADUATE", 146
            );

            CourseResponse c1 = courseService.createCourse(new CreateCourseRequest(
                    "CS-CURR-1", "Programming 1", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));
            CourseResponse c2 = courseService.createCourse(new CreateCourseRequest(
                    "CS-CURR-2", "Programming 2", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));

            // Create Curriculum (Initial status: DRAFT, v1)
            CurriculumResponse curr = curriculumService.createCurriculum(new CreateCurriculumRequest(
                    prog.id(), "BSIT-2026-DRAFT", "BSIT 2026 Draft", "2026-2027"
            ));
            assertThat(curr.status()).isEqualTo("DRAFT");
            assertThat(curr.versionNumber()).isEqualTo(1);

            // Assign courses
            CurriculumCourseResponse cc1 = curriculumService.assignCourseToCurriculum(curr.id(),
                    new AssignCourseToCurriculumRequest(c1.id(), 1, "1ST_SEM", "PROFESSIONAL_MAJOR", null));
            assertThat(cc1.sequenceOrder()).isEqualTo(1);

            CurriculumCourseResponse cc2 = curriculumService.assignCourseToCurriculum(curr.id(),
                    new AssignCourseToCurriculumRequest(c2.id(), 1, "1ST_SEM", "PROFESSIONAL_MAJOR", null));
            assertThat(cc2.sequenceOrder()).isEqualTo(2);

            // Transition status to ACTIVE (Locked)
            curriculumService.transitionStatus(curr.id(), Curriculum.Status.UNDER_REVIEW);
            curriculumService.transitionStatus(curr.id(), Curriculum.Status.APPROVED);
            curriculumService.transitionStatus(curr.id(), Curriculum.Status.ACTIVE);

            // Immutability Guard: Reject modifying locked/active curriculum details
            assertThatThrownBy(() -> curriculumService.updateCurriculum(curr.id(),
                    new UpdateCurriculumRequest("Renamed While Active", "2026-2027")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Curriculum is locked under status: ACTIVE");

            // Immutability Guard: Reject assigning courses to locked curriculum
            CourseResponse c3 = courseService.createCourse(new CreateCourseRequest(
                    "CS-CURR-3", "Algorithms", new BigDecimal("3.00"), BigDecimal.ZERO, 3, 0, null
            ));
            assertThatThrownBy(() -> curriculumService.assignCourseToCurriculum(curr.id(),
                    new AssignCourseToCurriculumRequest(c3.id(), 1, "2ND_SEM", "PROFESSIONAL_MAJOR", null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Curriculum is locked");

            // Deleting active curriculum archives it
            curriculumService.deleteCurriculum(curr.id());
            assertThat(curriculumService.getCurriculumById(curr.id()).status()).isEqualTo("ARCHIVED");
        }
    }

    // =========================================================================
    // 5. GRADING & FINANCIAL FOUNDATIONS
    // =========================================================================
    @Nested
    @DisplayName("5. Grading & Financial Foundations")
    class GradingAndFinancialFoundationsTests {

        @Test
        @DisplayName("GradingScale CRUD: Range bounds check, transmutation lookup, and bracket updates")
        void testGradingScale_CrudAndTransmutation() {
            // Rejects invalid percentages (e.g. min > max)
            assertThatThrownBy(() -> gradingScaleService.createGradingScale(
                    "INVALID-SCALE",
                    new BigDecimal("1.00"),
                    new BigDecimal("95.00"),
                    new BigDecimal("90.00"), // min > max
                    "1.00",
                    "Excellent",
                    true,
                    false
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Percentage min cannot exceed percentage max");

            // Create valid grading scale
            GradingScale scale = gradingScaleService.createGradingScale(
                    "GRADE-1.00",
                    new BigDecimal("1.00"),
                    new BigDecimal("97.50"),
                    new BigDecimal("100.00"),
                    "1.00",
                    "Excellent",
                    true,
                    false
            );
            assertThat(scale.getId()).isNotNull();

            // Update bracket
            GradingScale updated = gradingScaleService.updateGradingScale(
                    scale.getId(),
                    new BigDecimal("98.00"),
                    new BigDecimal("100.00"),
                    "Outstanding Performance",
                    true
            );
            assertThat(updated.getPercentageMin()).isEqualByComparingTo(new BigDecimal("98.00"));
            assertThat(updated.getRemarks()).isEqualTo("Outstanding Performance");

            // Transmutation test
            GradingScale transmuted = transmutationService.transmutePercentage(new BigDecimal("99.50"));
            assertThat(transmuted.getRemarks()).contains("Outstanding");
        }

        @Test
        @DisplayName("Financial Foundations: FeeCategory, FeeCatalog, PaymentTermTemplate, ScholarshipDiscount")
        void testFinancial_CompleteCrudAndGuards() {
            // 1. FeeCategory CRUD
            FeeCategory cat = financialService.createFeeCategory(FeeCategoryCode.LABORATORY, "Laboratory Computer Fees");
            assertThat(cat.getId()).isNotNull();

            // 2. FeeCatalog CRUD with Category Precondition
            assertThatThrownBy(() -> financialService.createFeeCatalog(
                    999999L, "FEE-FAIL", "Fail Fee", new BigDecimal("500.00"), false, true, true
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Fee category not found");

            FeeCatalog fee = financialService.createFeeCatalog(
                    cat.getId(), "FEE-COMP-LAB", "Computer Laboratory Usage", new BigDecimal("450.00"), false, true, true
            );
            assertThat(fee.getId()).isNotNull();

            // Deletion guard: Cannot delete FeeCategory if FeeCatalog references it
            assertThatThrownBy(() -> financialService.deleteFeeCategory(cat.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete fee category containing active catalog fee items");

            // Update fee pricing
            FeeCatalog updatedFee = financialService.updateFeeCatalogPricing(fee.getId(), "Computer Lab Advanced", new BigDecimal("550.00"), false);
            assertThat(updatedFee.getDefaultAmount()).isEqualByComparingTo(new BigDecimal("550.00"));

            // 3. PaymentTermTemplate: Strict 100.00% sum validation
            assertThatThrownBy(() -> financialService.createPaymentTermTemplate(
                    "TEMPLATE-FAIL",
                    new BigDecimal("30.00"),
                    new BigDecimal("20.00"),
                    new BigDecimal("20.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("20.00") // Sum = 90% != 100%
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("strictly equal 100.00%");

            PaymentTermTemplate template = financialService.createPaymentTermTemplate(
                    "STANDARD-4-INSTALLMENT",
                    new BigDecimal("25.00"),
                    new BigDecimal("25.00"),
                    new BigDecimal("25.00"),
                    BigDecimal.ZERO,
                    new BigDecimal("25.00") // Sum = 100%
            );
            assertThat(template.getId()).isNotNull();

            // 4. ScholarshipDiscount CRUD
            ScholarshipDiscount scholarship = financialService.createScholarshipDiscount(
                    "SCHOLAR-ACAD-FULL",
                    "President's Honor List Full Scholarship",
                    ScholarshipType.ACADEMIC_FULL,
                    ScholarshipCategory.INSTITUTIONAL,
                    new BigDecimal("100.00"),
                    BigDecimal.ZERO,
                    "Institutional General Fund",
                    true,
                    true
            );
            assertThat(scholarship.getId()).isNotNull();
            assertThat(scholarship.getDiscountPercentage()).isEqualByComparingTo(new BigDecimal("100.00"));

            // Update Scholarship discount rule
            ScholarshipDiscount updatedScholar = financialService.updateScholarshipDiscount(
                    scholarship.getId(),
                    "President's Honor List Partial Scholarship",
                    ScholarshipCategory.INSTITUTIONAL,
                    ScholarshipType.ACADEMIC_FULL,
                    new BigDecimal("75.00"),
                    BigDecimal.ZERO,
                    "Institutional General Fund",
                    true,
                    false
            );
            assertThat(updatedScholar.getDiscountPercentage()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(updatedScholar.isAppliesToMisc()).isFalse();

            // Clean deletes
            financialService.deleteFeeCatalog(fee.getId());
            financialService.deleteFeeCategory(cat.getId());
            financialService.deletePaymentTermTemplate(template.getId());
            financialService.deleteScholarshipDiscount(scholarship.getId());
        }
    }
}
