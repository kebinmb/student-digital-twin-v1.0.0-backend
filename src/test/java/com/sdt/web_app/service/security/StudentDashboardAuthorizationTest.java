package com.sdt.web_app.service.security;

import com.sdt.web_app.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentDashboardAuthorizationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("STUDENT role should be allowed to access /api/v1/curricula/lookup (200 OK)")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentCanAccessCurriculaLookup() throws Exception {
        mockMvc.perform(get("/api/v1/curricula/lookup"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("STUDENT role should be allowed to access /api/v1/campuses (200 OK)")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentCanAccessCampuses() throws Exception {
        mockMvc.perform(get("/api/v1/campuses"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("STUDENT role should be allowed to access /api/v1/scheduling/sections/term/1 (200 OK)")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentCanAccessSchedulingSectionsByTerm() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("REGRESSION: STUDENT role must be forbidden from administrative user management (/api/v1/admin/users) (403 Forbidden)")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentCannotAccessAdminUserManagement() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REGRESSION: STUDENT role must be forbidden from UniFAST financial claims (/api/v1/finance/unifast/claims/term/1) (403 Forbidden)")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentCannotAccessUnifastClaims() throws Exception {
        mockMvc.perform(get("/api/v1/finance/unifast/claims/term/1"))
                .andExpect(status().isForbidden());
    }
}
