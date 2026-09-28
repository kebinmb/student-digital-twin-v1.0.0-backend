package com.sdt.web_app.service;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.analytics.StudentRiskScoreRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class HappyPathLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentRiskScoreRepository studentRiskScoreRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Test
    @DisplayName("Verify End-to-End Happy Path Student Digital Twin Lifecycle")
    void testEndToEndStudentDigitalTwinLifecycle() {
        // 1. Create or fetch Student Profile with valid campus, department, program & curriculum linkage
        StudentProfile student = studentProfileRepository.findAll().stream().findFirst().orElseGet(() -> {
            User user = userRepository.save(User.builder()
                    .username("happy_path_student_" + System.currentTimeMillis())
                    .email("happy.student@chmsu.edu.ph")
                    .password("encoded_secret_password")
                    .roles(Set.of(Roles.STUDENT))
                    .build());

            Campus campus = campusRepository.save(Campus.builder()
                    .code("CAMPUS_HP_" + System.currentTimeMillis() % 1000)
                    .name("Happy Path Main Campus")
                    .build());

            Department dept = departmentRepository.save(Department.builder()
                    .campus(campus)
                    .code("DEPT_HP_" + System.currentTimeMillis() % 1000)
                    .name("Happy Path Department")
                    .build());

            Program prog = programRepository.save(Program.builder()
                    .code("PROG_HP_" + System.currentTimeMillis() % 1000)
                    .name("Happy Path BSIT")
                    .department(dept)
                    .build());

            Curriculum curr = curriculumRepository.save(Curriculum.builder()
                    .program(prog)
                    .code("CURR_HP_" + System.currentTimeMillis() % 1000)
                    .name("2026 Happy Path Curriculum")
                    .effectiveAcademicYear("2026-2027")
                    .build());

            return studentProfileRepository.save(StudentProfile.builder()
                    .user(user)
                    .program(prog)
                    .curriculum(curr)
                    .studentNumber("2026-HP-" + System.currentTimeMillis() % 10000)
                    .firstName("Happy")
                    .lastName("Path")
                    .yearLevel(1)
                    .enrollmentStatus(StudentProfile.EnrollmentStatus.REGULAR)
                    .build());
        });

        assertThat(student).isNotNull();
        assertThat(student.getStudentNumber()).isNotBlank();

        // 2. Create Digital Twin Risk Telemetry Record
        StudentRiskScore riskScore = StudentRiskScore.builder()
                .student(student)
                .academicRiskScore(new BigDecimal("15.50"))
                .attendanceRiskScore(new BigDecimal("10.00"))
                .socioeconomicRiskScore(new BigDecimal("5.00"))
                .compositeRiskLevel(StudentRiskScore.RiskLevel.LOW)
                .predictedDropoutProbability(new BigDecimal("0.0500"))
                .recommendedInterventions("Routine academic advising monitoring")
                .evaluatedAt(Instant.now())
                .build();

        StudentRiskScore savedRisk = studentRiskScoreRepository.save(riskScore);
        assertThat(savedRisk.getId()).isNotNull();
        assertThat(savedRisk.getCompositeRiskLevel()).isEqualTo(StudentRiskScore.RiskLevel.LOW);

        // 3. Read back & update risk state
        savedRisk.updateEvaluation(
                new BigDecimal("12.00"),
                new BigDecimal("8.00"),
                new BigDecimal("5.00"),
                StudentRiskScore.RiskLevel.LOW,
                new BigDecimal("0.0300"),
                "Academic advising target achieved"
        );
        StudentRiskScore updatedRisk = studentRiskScoreRepository.save(savedRisk);
        assertThat(updatedRisk.getAcademicRiskScore()).isEqualByComparingTo("12.00");

        // 4. Verify clean querying without rollbacks
        assertThat(studentRiskScoreRepository.findTopByStudentIdOrderByEvaluatedAtDesc(student.getId())).isPresent();
    }
}
