package com.sdt.web_app.service.analytics;

import com.sdt.web_app.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FacultyAndStudentTelemetryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ROLE_FACULTY can access faculty section telemetry endpoint")
    @WithMockUser(username = "1", roles = {"FACULTY"})
    void testFacultyGetStudentTelemetrySuccess() throws Exception {
        mockMvc.perform(get("/api/v1/faculty/telemetry/students")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("ROLE_FACULTY can retrieve assigned section options")
    @WithMockUser(username = "1", roles = {"FACULTY"})
    void testFacultyGetAssignedSectionsSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/faculty/telemetry/sections"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_STUDENT is forbidden from accessing faculty telemetry endpoint")
    @WithMockUser(username = "2", roles = {"STUDENT"})
    void testStudentForbiddenFromFacultyEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/faculty/telemetry/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_STUDENT can access student self-telemetry endpoint")
    @WithMockUser(username = "1", roles = {"STUDENT"})
    void testStudentGetSelfTelemetrySuccess() throws Exception {
        mockMvc.perform(get("/api/v1/student/telemetry/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.wellnessScore").exists())
                .andExpect(jsonPath("$.dimensionScores").isMap());
    }

    @Test
    @DisplayName("ROLE_FACULTY can retrieve section telemetry KPI summary")
    @WithMockUser(username = "1", roles = {"FACULTY"})
    void testFacultyGetTelemetryKpiSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/faculty/telemetry/kpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonitored").exists())
                .andExpect(jsonPath("$.averageWellnessIndex").exists());
    }

    @Test
    @DisplayName("ROLE_STUDENT is forbidden from accessing faculty telemetry KPI endpoint")
    @WithMockUser(username = "2", roles = {"STUDENT"})
    void testStudentForbiddenFromFacultyKpiEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/faculty/telemetry/kpi"))
                .andExpect(status().isForbidden());
    }
}
