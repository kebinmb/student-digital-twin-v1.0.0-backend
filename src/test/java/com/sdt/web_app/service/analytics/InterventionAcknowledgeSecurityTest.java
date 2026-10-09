package com.sdt.web_app.service.analytics;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.entities.analytics.StudentIntervention;
import com.sdt.web_app.entities.analytics.StudentIntervention.InterventionStatus;
import com.sdt.web_app.entities.analytics.StudentIntervention.InterventionType;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.analytics.StudentInterventionRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class InterventionAcknowledgeSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentInterventionRepository interventionRepository;

    private StudentProfile testStudent;
    private StudentIntervention testIntervention;
    private String studentUserIdStr;

    @BeforeEach
    void setUp() {
        testStudent = studentProfileRepository.findAll().stream()
                .filter(s -> s.getUser() != null)
                .findFirst()
                .orElse(null);

        if (testStudent != null) {
            studentUserIdStr = String.valueOf(testStudent.getUser().getId());
            testIntervention = interventionRepository.save(
                    StudentIntervention.builder()
                            .student(testStudent)
                            .interventionType(InterventionType.GUIDANCE_COUNSELING)
                            .status(InterventionStatus.DISPATCHED)
                            .triggerFactor("Attendance warning flag")
                            .caseNotes("Initial counseling dispatch")
                            .build()
            );
        }
    }

    @Test
    @DisplayName("ROLE_STUDENT receives 200 OK when acknowledging own intervention")
    void studentShouldReceive200WhenAcknowledgingOwnIntervention() throws Exception {
        if (testIntervention == null) return;

        mockMvc.perform(post("/api/v1/student/telemetry/interventions/{id}/acknowledge", testIntervention.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(studentUserIdStr).roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\": \"I acknowledge this feedback.\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_STUDENT does not receive 403 Forbidden when accessing own intervention")
    void studentShouldNotReceive403WhenAccessingOwnIntervention() throws Exception {
        if (testIntervention == null) return;

        mockMvc.perform(post("/api/v1/student/telemetry/interventions/{id}/acknowledge", testIntervention.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(studentUserIdStr).roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated user receives 401 Unauthorized")
    void unauthenticatedUserShouldReceive401() throws Exception {
        Long interventionId = testIntervention != null ? testIntervention.getId() : 1L;

        mockMvc.perform(post("/api/v1/student/telemetry/interventions/{id}/acknowledge", interventionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_STUDENT receives 409 Conflict when attempting to acknowledge another student's intervention")
    void studentShouldReceive409WhenAcknowledgingOtherStudentsIntervention() throws Exception {
        if (testIntervention == null) return;

        // User ID 999999 does not match testIntervention's student
        mockMvc.perform(post("/api/v1/student/telemetry/interventions/{id}/acknowledge", testIntervention.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("999999").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\": \"Invalid attempt\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ROLE_STUDENT acknowledging already acknowledged intervention returns 200 OK (idempotent)")
    void studentAcknowledgingAlreadyAcknowledgedInterventionReturns200() throws Exception {
        if (testIntervention == null) return;

        // First acknowledgement
        mockMvc.perform(post("/api/v1/student/telemetry/interventions/{id}/acknowledge", testIntervention.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(studentUserIdStr).roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\": \"First acknowledgement\"}"))
                .andExpect(status().isOk());

        // Second acknowledgement (must be idempotent 200 OK, not 409)
        mockMvc.perform(post("/api/v1/student/telemetry/interventions/{id}/acknowledge", testIntervention.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(studentUserIdStr).roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\": \"Second acknowledgement\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_ADMIN or ROLE_FACULTY can also acknowledge intervention")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void adminCanAcknowledgeIntervention() throws Exception {
        if (testIntervention == null) return;

        mockMvc.perform(post("/api/v1/student/telemetry/interventions/{id}/acknowledge", testIntervention.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\": \"Staff verified\"}"))
                .andExpect(status().isOk());
    }
}
