package com.sdt.web_app.service.analytics;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class EarlyWarningRadarSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ROLE_FACULTY receives 200 OK when accessing early warning radar")
    @WithMockUser(username = "faculty_user", roles = {"FACULTY"})
    void facultyShouldReceive200WhenAccessingRadar() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/radar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_ADMIN receives 200 OK when accessing early warning radar")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void adminShouldReceive200WhenAccessingRadar() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/radar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_REGISTRAR receives 200 OK when accessing early warning radar")
    @WithMockUser(username = "registrar_user", roles = {"REGISTRAR"})
    void registrarShouldReceive200WhenAccessingRadar() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/radar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_STUDENT receives 403 Forbidden when accessing early warning radar")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldReceive403ForbiddenWhenAccessingRadar() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/radar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user receives 401 Unauthorized")
    void unauthenticatedUserShouldReceive401WhenAccessingRadar() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/radar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_STUDENT receives 403 Forbidden on /early-warning/slice")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldReceive403ForbiddenOnSlice() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/slice")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Autowired
    private com.sdt.web_app.repositories.enrollment.StudentProfileRepository studentProfileRepository;

    @Autowired
    private com.sdt.web_app.repositories.analytics.StudentRiskScoreRepository riskScoreRepository;

    @Test
    @DisplayName("ROLE_FACULTY receives 200 OK on /early-warning/slice")
    @WithMockUser(username = "faculty_user", roles = {"FACULTY"})
    void facultyShouldReceive200OnSlice() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/slice")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Data scoping: FACULTY only sees students enrolled in their assigned sections, while ADMIN sees all")
    void facultyShouldOnlySeeStudentsInAssignedSections_AdminSeesAll() throws Exception {
        StudentProfile studentInClass = studentProfileRepository.findByStudentNumber("2026-0001").orElse(null);
        StudentProfile studentNotInClass = studentProfileRepository.findByStudentNumber("2026-0004").orElse(null);

        if (studentInClass != null && studentNotInClass != null) {
            // Give both students CRITICAL risk score
            riskScoreRepository.save(
                    StudentRiskScore.builder()
                            .student(studentInClass)
                            .evaluatedAt(java.time.Instant.now())
                            .academicRiskScore(new java.math.BigDecimal("90.00"))
                            .attendanceRiskScore(new java.math.BigDecimal("85.00"))
                            .socioeconomicRiskScore(new java.math.BigDecimal("50.00"))
                            .compositeRiskLevel(StudentRiskScore.RiskLevel.CRITICAL)
                            .predictedDropoutProbability(new java.math.BigDecimal("0.8500"))
                            .build()
            );

            riskScoreRepository.save(
                    StudentRiskScore.builder()
                            .student(studentNotInClass)
                            .evaluatedAt(java.time.Instant.now())
                            .academicRiskScore(new java.math.BigDecimal("92.00"))
                            .attendanceRiskScore(new java.math.BigDecimal("88.00"))
                            .socioeconomicRiskScore(new java.math.BigDecimal("55.00"))
                            .compositeRiskLevel(StudentRiskScore.RiskLevel.CRITICAL)
                            .predictedDropoutProbability(new java.math.BigDecimal("0.8800"))
                            .build()
            );

            // 1. When faculty_smith (instructor of 2026-0001's section) calls radar
            mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/radar")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("faculty_smith").roles("FACULTY"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[?(@.studentNumber == '2026-0001')]").exists())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[?(@.studentNumber == '2026-0004')]").doesNotExist());

            // 2. When admin calls radar, both students must be visible
            mockMvc.perform(get("/api/v1/analytics/digital-twin/early-warning/radar")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin_sys").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[?(@.studentNumber == '2026-0001')]").exists())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[?(@.studentNumber == '2026-0004')]").exists());
        }
    }
}
