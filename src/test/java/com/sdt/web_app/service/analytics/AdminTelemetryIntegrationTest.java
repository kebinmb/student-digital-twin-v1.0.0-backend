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

public class AdminTelemetryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ROLE_ADMIN can retrieve paginated student telemetry records with risk levels & interventions")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testAdminGetStudentTelemetrySuccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/telemetry/students")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("ROLE_STUDENT is forbidden from accessing administrative telemetry endpoint")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void testStudentAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/telemetry/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can retrieve total telemetry KPI summary")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testAdminGetTelemetryKpiSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/telemetry/kpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonitored").exists())
                .andExpect(jsonPath("$.criticalRiskCount").exists())
                .andExpect(jsonPath("$.highRiskCount").exists())
                .andExpect(jsonPath("$.moderateRiskCount").exists())
                .andExpect(jsonPath("$.lowRiskCount").exists())
                .andExpect(jsonPath("$.totalActiveInterventions").exists())
                .andExpect(jsonPath("$.averageWellnessIndex").exists());
    }

    @Test
    @DisplayName("ROLE_STUDENT is forbidden from accessing administrative telemetry KPI endpoint")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void testStudentAccessKpiForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/telemetry/kpi"))
                .andExpect(status().isForbidden());
    }
}
