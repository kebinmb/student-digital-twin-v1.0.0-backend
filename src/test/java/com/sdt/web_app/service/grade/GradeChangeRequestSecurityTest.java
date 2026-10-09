package com.sdt.web_app.service.grade;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.dto.grade.GradeChangeDtos.GradeChangeResponse;
import com.sdt.web_app.service.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GradeChangeRequestSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeChangeService gradeChangeService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private GradeChangeResponse createMockResponse(Long id, Long programId) {
        return new GradeChangeResponse(
                id,
                10L,
                "2026-00001",
                "Juan Dela Cruz",
                programId,
                "BSIT",
                1L,
                101L,
                "IT101",
                "Intro to IT",
                1L,
                "FIRST_SEM",
                new BigDecimal("3.00"),
                new BigDecimal("1.75"),
                "Correction of computation",
                "PENDING",
                "faculty1",
                null,
                Instant.now()
        );
    }

    @Test
    @DisplayName("ROLE_CHAIRPERSON receives 200 OK when accessing pending grade change requests")
    @WithMockUser(username = "chairperson_user", roles = {"CHAIRPERSON"})
    void chairpersonShouldReceive200ForPendingChangeRequests() throws Exception {
        given(gradeChangeService.getPendingRequests(any())).willReturn(List.of(createMockResponse(1L, 5L)));

        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].programId").value(5));
    }

    @Test
    @DisplayName("ROLE_ADMIN receives 200 OK when accessing pending grade change requests")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void adminShouldReceive200ForPendingChangeRequests() throws Exception {
        given(gradeChangeService.getPendingRequests(any())).willReturn(List.of(createMockResponse(1L, 5L)));

        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_REGISTRAR receives 200 OK when accessing pending grade change requests")
    @WithMockUser(username = "registrar_user", roles = {"REGISTRAR"})
    void registrarShouldReceive200ForPendingChangeRequests() throws Exception {
        given(gradeChangeService.getPendingRequests(any())).willReturn(List.of(createMockResponse(1L, 5L)));

        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_DEAN receives 200 OK when accessing pending grade change requests")
    @WithMockUser(username = "dean_user", roles = {"DEAN"})
    void deanShouldReceive200ForPendingChangeRequests() throws Exception {
        given(gradeChangeService.getPendingRequests(any())).willReturn(List.of(createMockResponse(1L, 5L)));

        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_FACULTY receives 200 OK when accessing pending grade change requests")
    @WithMockUser(username = "faculty_user", roles = {"FACULTY"})
    void facultyShouldReceive200ForPendingChangeRequests() throws Exception {
        given(gradeChangeService.getPendingRequests(any())).willReturn(List.of(createMockResponse(1L, 5L)));

        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_STUDENT receives 403 Forbidden when accessing pending grade change requests")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldReceive403ForPendingChangeRequests() throws Exception {
        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user receives 401 Unauthorized")
    void unauthenticatedShouldReceive401ForPendingChangeRequests() throws Exception {
        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_CHAIRPERSON receives program-scoped results with termId query param")
    @WithMockUser(username = "chairperson_user", roles = {"CHAIRPERSON"})
    void chairpersonShouldReceiveProgramScopedResultsWithTermId() throws Exception {
        given(gradeChangeService.getPendingRequests(50L)).willReturn(List.of(createMockResponse(2L, 5L)));

        mockMvc.perform(get("/api/v1/grades/change-requests/pending")
                        .param("termId", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].programId").value(5));
    }
}
