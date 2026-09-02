package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.ValidationReportDto;

import com.sdt.web_app.entities.institution.*;

import com.sdt.web_app.repositories.institution.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CurriculumValidationTest {

    @Autowired private CurriculumValidationService validationService;
    @Autowired private CurriculumRepository curriculumRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CurriculumCourseRepository curriculumCourseRepository;
    @Autowired private CoursePrerequisiteRepository prerequisiteRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private CampusRepository campusRepository;

    private Curriculum curriculum;
    private Course courseA;
    private Course courseB;

    @BeforeEach
    void setup() {
        // 1. Ensure a Campus exists
        Campus campus = campusRepository.findAll().stream().findFirst().orElseGet(() ->
                campusRepository.save(Campus.builder()
                        .code("TALISAY")
                        .name("CHMSU - Talisay Main")
                        .region("REGION VI")
                        .build())
        );

        // 2. Ensure a Department exists
        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() ->
                departmentRepository.save(Department.builder()
                        .campus(campus)
                        .code("CCS")
                        .name("College of Computer Studies")
                        .type(DepartmentType.COLLEGE)
                        .build())
        );

        // 3. Ensure a Program exists (prevents NoSuchElementException)
        Program program = programRepository.findAll().stream().findFirst().orElseGet(() ->
                programRepository.save(Program.builder()
                        .department(department)
                        .code("BSIT")
                        .name("Bachelor of Science in Information Technology")
                        .degreeLevel("UNDERGRADUATE")
                        .totalUnitsRequired(6) // 3 + 3 for courseA and courseB
                        .build())
        );

        // 4. Create Curriculum under test
        curriculum = curriculumRepository.save(Curriculum.builder()
                .code("TEST-CURR-01")
                .name("Test Verification Curriculum")
                .program(program)
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.DRAFT)
                .build());

        // 5. Create Courses
        courseA = courseRepository.save(Course.builder()
                .code("TEST 101")
                .title("Subject A")
                .lectureUnits(new BigDecimal("3.00"))
                .labUnits(BigDecimal.ZERO)
                .creditUnits(new BigDecimal("3.00"))
                .contactHoursLec(3)
                .contactHoursLab(0)
                .build());

        courseB = courseRepository.save(Course.builder()
                .code("TEST 102")
                .title("Subject B")
                .lectureUnits(new BigDecimal("3.00"))
                .labUnits(BigDecimal.ZERO)
                .creditUnits(new BigDecimal("3.00"))
                .contactHoursLec(3)
                .contactHoursLab(0)
                .build());

        curriculumCourseRepository.save(CurriculumCourse.builder()
                .curriculum(curriculum).course(courseA).yearLevel(1).semester("1ST_SEM").sequenceOrder(1).build());
        curriculumCourseRepository.save(CurriculumCourse.builder()
                .curriculum(curriculum).course(courseB).yearLevel(1).semester("2ND_SEM").sequenceOrder(2).build());
    }

    @Test
    @DisplayName("Should detect circular dependency: Course A -> Course B -> Course A")
    void shouldDetectCircularPrerequisiteDependency() {
        // Course B requires Course A
        prerequisiteRepository.save(CoursePrerequisite.builder()
                .course(courseB)
                .prerequisiteCourse(courseA)
                .ruleType("HARD")
                .build());

        // Course A requires Course B (creates cycle)
        prerequisiteRepository.save(CoursePrerequisite.builder()
                .course(courseA)
                .prerequisiteCourse(courseB)
                .ruleType("HARD")
                .build());

        ValidationReportDto report = validationService.validateCurriculum(curriculum.getId());

        assertThat(report.valid()).isFalse();
        assertThat(report.errors())
                .anyMatch(e -> "CIRCULAR_DEPENDENCY_DETECTED".equals(e.code()));
    }
}