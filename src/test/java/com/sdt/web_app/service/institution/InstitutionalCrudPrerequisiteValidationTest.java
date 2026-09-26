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
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.institution.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InstitutionalCrudPrerequisiteValidationTest {

    @Autowired private AcademicYearService academicYearService;
    @Autowired private CampusService campusService;
    @Autowired private DepartmentService departmentService;
    @Autowired private CourseService courseService;
    @Autowired private CourseOutcomeService courseOutcomeService;
    @Autowired private CoursePrerequisiteService prerequisiteService;
    @Autowired private CiloPiloMappingService mappingService;
    @Autowired private CurriculumService curriculumService;

    @Autowired private CampusRepository campusRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private ProgramOutcomeRepository programOutcomeRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AcademicYearRepository academicYearRepository;

    private Campus testCampus;
    private Department testDept;
    private Program testProgram;
    private ProgramOutcome testPilo;
    private Course testCourse1;
    private Course testCourse2;

    @BeforeEach
    void setUp() {
        testCampus = campusRepository.findAll().stream().findFirst().orElseGet(() ->
                campusRepository.save(Campus.builder()
                        .code("TALISAY-MAIN")
                        .name("CHMSU - Talisay Main")
                        .region("REGION VI")
                        .isMain(true)
                        .build())
        );

        testDept = departmentRepository.findAll().stream().findFirst().orElseGet(() ->
                departmentRepository.save(Department.builder()
                        .campus(testCampus)
                        .code("CCS-TEST")
                        .name("College of Computer Studies")
                        .type(DepartmentType.COLLEGE)
                        .build())
        );

        testProgram = programRepository.findAll().stream().findFirst().orElseGet(() ->
                programRepository.save(Program.builder()
                        .department(testDept)
                        .code("BSIT-TEST")
                        .name("Bachelor of Science in Information Technology")
                        .degreeLevel("UNDERGRADUATE")
                        .totalUnitsRequired(146)
                        .build())
        );

        testPilo = programOutcomeRepository.findAll().stream().findFirst().orElseGet(() ->
                programOutcomeRepository.save(ProgramOutcome.builder()
                        .program(testProgram)
                        .code("PILO-01")
                        .description("Apply knowledge of computing fundamentals.")
                        .build())
        );

        testCourse1 = courseRepository.save(Course.builder()
                .code("CS-101-TEST")
                .title("Programming 1")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .contactHoursLec(2)
                .contactHoursLab(3)
                .build());

        testCourse2 = courseRepository.save(Course.builder()
                .code("CS-102-TEST")
                .title("Data Structures")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .contactHoursLec(2)
                .contactHoursLab(3)
                .build());
    }

    // -------------------------------------------------------------------------
    // 1. Curriculum Prerequisite Enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Curriculum creation fails with 400 (IllegalArgumentException) when programId does not exist")
    void createCurriculum_NonExistentProgram_ThrowsException() {
        CreateCurriculumRequest request = new CreateCurriculumRequest(
                999999L, // Non-existent program ID
                "CURR-INVALID",
                "Invalid Curriculum",
                "2026-2027"
        );

        assertThatThrownBy(() -> curriculumService.createCurriculum(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Program not found with ID");
    }

    @Test
    @DisplayName("Curriculum creation succeeds when valid programId is provided")
    void createCurriculum_ValidProgram_Success() {
        CreateCurriculumRequest request = new CreateCurriculumRequest(
                testProgram.getId(),
                "BSIT-CURR-2026",
                "BSIT Curriculum 2026 Revision",
                "2026-2027"
        );

        CurriculumResponse response = curriculumService.createCurriculum(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.code()).isEqualTo("BSIT-CURR-2026");
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.versionNumber()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 2. Department Prerequisite Enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Department creation fails when campusId does not exist")
    void createDepartment_NonExistentCampus_ThrowsException() {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                888888L, // Non-existent campus ID
                "DEPT-INV",
                "Invalid Department",
                DepartmentType.COLLEGE,
                null,
                null
        );

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campus not found with ID");
    }

    @Test
    @DisplayName("Department creation fails when parent department belongs to a different campus")
    void createDepartment_ParentFromDifferentCampus_ThrowsException() {
        Campus otherCampus = campusRepository.save(Campus.builder()
                .code("BINALBAGAN")
                .name("CHMSU - Binalbagan Campus")
                .region("REGION VI")
                .build());

        Department otherDept = departmentRepository.save(Department.builder()
                .campus(otherCampus)
                .code("COE-BIN")
                .name("College of Education Binalbagan")
                .type(DepartmentType.COLLEGE)
                .build());

        CreateDepartmentRequest request = new CreateDepartmentRequest(
                testCampus.getId(),
                "NEW-DEPT",
                "New Department",
                DepartmentType.DEPARTMENT,
                otherDept.getId(), // parent from other campus!
                null
        );

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent department must belong to the same campus");
    }

    // -------------------------------------------------------------------------
    // 3. Course Prerequisite Validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CoursePrerequisite creation fails when courseId == prerequisiteCourseId")
    void createPrerequisite_SelfPrerequisite_ThrowsException() {
        CreateCoursePrerequisiteRequest request = new CreateCoursePrerequisiteRequest(
                testCourse1.getId(),
                testCourse1.getId(), // Self-prerequisite!
                "HARD",
                "3.00"
        );

        assertThatThrownBy(() -> prerequisiteService.createPrerequisite(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Course cannot be its own prerequisite");
    }

    @Test
    @DisplayName("CoursePrerequisite creation fails when circular dependency is introduced")
    void createPrerequisite_CircularDependency_ThrowsException() {
        // Course 2 requires Course 1
        prerequisiteService.createPrerequisite(new CreateCoursePrerequisiteRequest(
                testCourse2.getId(),
                testCourse1.getId(),
                "HARD",
                "3.00"
        ));

        // Now attempt Course 1 requires Course 2 -> forms a cycle!
        CreateCoursePrerequisiteRequest cyclicRequest = new CreateCoursePrerequisiteRequest(
                testCourse1.getId(),
                testCourse2.getId(),
                "HARD",
                "3.00"
        );

        assertThatThrownBy(() -> prerequisiteService.createPrerequisite(cyclicRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("circular dependency");
    }

    @Test
    @DisplayName("CoursePrerequisite creation allows mutual CO_REQUISITE relationships without cycle errors")
    void createPrerequisite_MutualCoRequisite_Succeeds() {
        // Course 2 co-requires Course 1
        var resp1 = prerequisiteService.createPrerequisite(new CreateCoursePrerequisiteRequest(
                testCourse2.getId(),
                testCourse1.getId(),
                "CO_REQUISITE",
                "3.00"
        ));
        assertThat(resp1).isNotNull();

        // Mutual co-requisite: Course 1 co-requires Course 2 -> should succeed without cycle error
        var resp2 = prerequisiteService.createPrerequisite(new CreateCoursePrerequisiteRequest(
                testCourse1.getId(),
                testCourse2.getId(),
                "CO_REQUISITE",
                "3.00"
        ));
        assertThat(resp2).isNotNull();
    }

    // -------------------------------------------------------------------------
    // 4. CILO-PILO Mapping Prerequisite Enforcement
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CiloPiloMapping creation fails when courseOutcomeId does not exist")
    void createMapping_NonExistentCourseOutcome_ThrowsException() {
        CreateCiloPiloMappingRequest request = new CreateCiloPiloMappingRequest(
                777777L, // Non-existent CILO ID
                testPilo.getId(),
                "I"
        );

        assertThatThrownBy(() -> mappingService.createOrUpdateMapping(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Course outcome not found with ID");
    }

    @Test
    @DisplayName("CiloPiloMapping creation fails when programOutcomeId does not exist")
    void createMapping_NonExistentProgramOutcome_ThrowsException() {
        CourseOutcomeResponse cilo = courseOutcomeService.createCourseOutcome(testCourse1.getId(),
                new CreateCourseOutcomeRequest("CILO-01", "Understand fundamentals", "Remembering"));

        CreateCiloPiloMappingRequest request = new CreateCiloPiloMappingRequest(
                cilo.id(),
                666666L, // Non-existent PILO ID
                "E"
        );

        assertThatThrownBy(() -> mappingService.createOrUpdateMapping(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Program outcome not found with ID");
    }

    @Test
    @DisplayName("CiloPiloMapping creation and upsert succeeds for valid IDs")
    void createMapping_ValidOutcomeIds_Success() {
        CourseOutcomeResponse cilo = courseOutcomeService.createCourseOutcome(testCourse1.getId(),
                new CreateCourseOutcomeRequest("CILO-02", "Apply core concepts", "Applying"));

        // First mapping creation
        CiloPiloMappingResponse response = mappingService.createOrUpdateMapping(
                new CreateCiloPiloMappingRequest(cilo.id(), testPilo.getId(), "I")
        );
        assertThat(response.mappingType()).isEqualTo("I");

        // Upsert to Emphasized (E)
        CiloPiloMappingResponse updated = mappingService.createOrUpdateMapping(
                new CreateCiloPiloMappingRequest(cilo.id(), testPilo.getId(), "E")
        );
        assertThat(updated.id()).isEqualTo(response.id());
        assertThat(updated.mappingType()).isEqualTo("E");
    }

    // -------------------------------------------------------------------------
    // 5. Academic Year Lifecycle & Deletion Guard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AcademicYear creation fails when endDate is before startDate")
    void createAcademicYear_InvalidDateRange_ThrowsException() {
        CreateAcademicYearRequest request = new CreateAcademicYearRequest(
                "AY 2030-2031",
                LocalDate.of(2030, 8, 1),
                LocalDate.of(2030, 5, 1), // Invalid: before start date
                false
        );

        assertThatThrownBy(() -> academicYearService.createAcademicYear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");
    }

    @Test
    @DisplayName("AcademicYear setting current atomically demotes existing active year")
    void setCurrentAcademicYear_DemotesExistingCurrent() {
        AcademicYearResponse ay1 = academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                "AY 2028-2029",
                LocalDate.of(2028, 8, 1),
                LocalDate.of(2029, 6, 30),
                true
        ));
        assertThat(ay1.isCurrent()).isTrue();

        AcademicYearResponse ay2 = academicYearService.createAcademicYear(new CreateAcademicYearRequest(
                "AY 2029-2030",
                LocalDate.of(2029, 8, 1),
                LocalDate.of(2030, 6, 30),
                false
        ));
        assertThat(ay2.isCurrent()).isFalse();

        // Promote ay2 to current
        academicYearService.setCurrentAcademicYear(ay2.id());

        assertThat(academicYearService.getAcademicYearById(ay2.id()).isCurrent()).isTrue();
        assertThat(academicYearService.getAcademicYearById(ay1.id()).isCurrent()).isFalse();
    }

    // -------------------------------------------------------------------------
    // 6. Course Lifecycle & Assignment
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Course creation calculates creditUnits = lectureUnits + labUnits")
    void createCourse_CalculatesCreditUnits() {
        CourseResponse course = courseService.createCourse(new CreateCourseRequest(
                "CS-201-UNIT",
                "Object-Oriented Programming",
                new BigDecimal("2.00"),
                new BigDecimal("1.00"),
                2,
                3,
                "OOP Principles"
        ));

        assertThat(course.creditUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(course.isActive()).isTrue();
    }

    @Test
    @DisplayName("Assigning course to curriculum succeeds and calculates sequence order")
    void assignCourseToCurriculum_Success() {
        CurriculumResponse curr = curriculumService.createCurriculum(new CreateCurriculumRequest(
                testProgram.getId(),
                "BSIT-ASSIGN-TEST",
                "Assign Test Curriculum",
                "2026-2027"
        ));

        CurriculumCourseResponse cc = curriculumService.assignCourseToCurriculum(curr.id(),
                new AssignCourseToCurriculumRequest(
                        testCourse1.getId(),
                        1,
                        "1ST_SEM",
                        "PROFESSIONAL_MAJOR",
                        null // Auto-assign sequenceOrder
                ));

        assertThat(cc.id()).isNotNull();
        assertThat(cc.sequenceOrder()).isEqualTo(1);
        assertThat(cc.courseCode()).isEqualTo("CS-101-TEST");
    }
}
