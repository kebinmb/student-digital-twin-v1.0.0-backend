package com.sdt.web_app;

import com.sdt.web_app.dto.compliance.ComplianceDtos.DegreeAuditResultDto;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.AdvisingEligibilityResponse;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentProfileResponse;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.*;
import com.sdt.web_app.service.compliance.DegreeAuditService;
import com.sdt.web_app.service.enrollment.EnrollmentService;
import com.sdt.web_app.service.enrollment.StudentService;
import com.sdt.web_app.utils.NameUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class StudentNameResolutionTest extends BaseIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private DegreeAuditService degreeAuditService;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    private Long testStudentId;
    private Long testTermId;

    // ─── NameUtil ──────────────────────────────────────────────────────────

    @Test
    void buildFullNameWithMiddleName() {
        String result = NameUtil.buildFullName("Juan", "Protacio", "Dela Cruz");
        assertThat(result).isEqualTo("Juan P. Dela Cruz");
    }

    @Test
    void buildFullNameWithoutMiddleName() {
        String result = NameUtil.buildFullName("Juan", null, "Dela Cruz");
        assertThat(result).isEqualTo("Juan Dela Cruz");
    }

    @Test
    void buildFullNameWithBlankMiddleName() {
        String result = NameUtil.buildFullName("Juan", "   ", "Dela Cruz");
        assertThat(result).isEqualTo("Juan Dela Cruz");
    }

    @Test
    void buildFullNameHandlesNullFirstName() {
        String result = NameUtil.buildFullName(null, null, "Dela Cruz");
        assertThat(result).isEqualTo("Dela Cruz");
    }

    @BeforeEach
    void setUpTestData() {
        String uid = UUID.randomUUID().toString().substring(0, 6);

        Campus campus = campusRepository.save(Campus.builder()
                .code("CAMPUS-" + uid)
                .name("Test Campus " + uid)
                .region("Region VI")
                .build());

        Department college = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("CCS-" + uid)
                .name("College of Computer Studies " + uid)
                .type(DepartmentType.COLLEGE)
                .build());

        Department dept = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("IT-" + uid)
                .name("IT Department " + uid)
                .type(DepartmentType.DEPARTMENT)
                .parentDepartment(college)
                .build());

        Program program = programRepository.save(Program.builder()
                .department(dept)
                .college(college)
                .code("BSCS-" + uid)
                .name("BS Computer Science " + uid)
                .degreeLevel("UNDERGRADUATE")
                .totalUnitsRequired(140)
                .build());

        AcademicYear ay = academicYearRepository.save(AcademicYear.builder()
                .code("AY-2026-" + uid)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2027, 5, 30))
                .isCurrent(true)
                .build());

        Term term = termRepository.save(Term.builder()
                .academicYear(ay)
                .termType(TermType.FIRST_SEM)
                .startDate(LocalDate.of(2026, 8, 15))
                .endDate(LocalDate.of(2026, 12, 20))
                .isActive(true)
                .maxHoursPerClass(new BigDecimal("3.0"))
                .build());
        testTermId = term.getId();

        Curriculum curriculum = curriculumRepository.save(Curriculum.builder()
                .program(program)
                .code("CURR-BSCS-" + uid)
                .name("BSCS Curriculum " + uid)
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.ACTIVE)
                .versionNumber(1)
                .build());

        User user = userRepository.save(User.builder()
                .username("juan_delacruz_" + uid)
                .email("juan_" + uid + "@chmsu.edu.ph")
                .password("hash")
                .enabled(true)
                .college(college)
                .program(program)
                .roles(Set.of(Roles.STUDENT))
                .build());

        StudentProfile profile = studentProfileRepository.save(StudentProfile.builder()
                .user(user)
                .studentNumber("STU-" + uid)
                .firstName("Juan")
                .middleName("Protacio")
                .lastName("Dela Cruz")
                .program(program)
                .curriculum(curriculum)
                .build());
        testStudentId = profile.getId();
    }

    // ─── Profile endpoint returns fullName from student_profiles ──────────────

    @Test
    void studentProfileResponseShouldContainFullNameFromProfileTable() {
        StudentProfileResponse response = studentService.getStudentById(testStudentId);

        // Must NOT be the username
        assertThat(response.fullName()).doesNotContain("student");
        assertThat(response.fullName()).doesNotContain("@");
        assertThat(response.fullName()).doesNotContain("_");

        // Must contain parts from student_profiles
        assertThat(response.fullName()).isNotBlank();
        assertThat(response.fullName()).isEqualTo("Juan P. Dela Cruz");
    }

    // ─── Advising studentName comes from student_profiles ─────────────────────

    @Test
    void advisingResponseShouldContainFullNameFromProfileTable() {
        AdvisingEligibilityResponse response = enrollmentService.getAdvisingEligibility(testStudentId, testTermId);

        assertThat(response.studentName()).isNotBlank();
        assertThat(response.studentName()).doesNotContain("_");
        assertThat(response.studentName()).isEqualTo("Juan P. Dela Cruz");
    }

    // ─── Audit studentName comes from student_profiles ────────────────────────

    @Test
    void auditResponseShouldContainFullNameFromProfileTable() {
        DegreeAuditResultDto response = degreeAuditService.evaluateDegreeAudit(testStudentId);

        assertThat(response.studentName()).isNotBlank();
        assertThat(response.studentName()).doesNotContain("_");
        assertThat(response.studentName()).isEqualTo("Juan P. Dela Cruz");
    }

    // ─── All endpoints must agree on the same name ────────────────────────────

    @Test
    void allEndpointsMustReturnSameFullNameForSameStudent() {
        String profileName = studentService.getStudentById(testStudentId).fullName();
        String advisingName = enrollmentService.getAdvisingEligibility(testStudentId, testTermId).studentName();
        String auditName = degreeAuditService.evaluateDegreeAudit(testStudentId).studentName();

        // All three must be identical — one source of truth
        assertThat(profileName).isEqualTo(advisingName);
        assertThat(profileName).isEqualTo(auditName);
    }
}
