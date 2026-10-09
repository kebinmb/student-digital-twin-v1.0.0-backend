package com.sdt.web_app.service.security;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.dto.compliance.EquityProfileAccountingView;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentEnrollmentResponse;
import com.sdt.web_app.dto.institution.CampusDtos.CampusResponse;
import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.CurriculumLookupOption;
import com.sdt.web_app.dto.scheduling.SchedulingDtos.SectionDetailResponse;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.service.compliance.StudentEquityProfileService;
import com.sdt.web_app.service.enrollment.EnrollmentService;
import com.sdt.web_app.service.institution.CampusService;
import com.sdt.web_app.service.institution.CurriculumDesignerService;
import com.sdt.web_app.service.scheduling.SchedulingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountantEndpointSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurriculumDesignerService designerService;

    @MockitoBean
    private SchedulingService schedulingService;

    @MockitoBean
    private CampusService campusService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private StudentEquityProfileService equityProfileService;

    @MockitoBean
    private SecurityUtils securityUtils;

    @MockitoBean
    private StudentProfileRepository studentProfileRepository;

    // ─── All 6 endpoints — ACCOUNTANT gets 200 OK ─────────────────────────────

    @Test
    @DisplayName("ACCOUNTANT can access Curricula Lookup (200 OK)")
    @WithMockUser(username = "accountant_user", roles = {"ACCOUNTANT"})
    void accountantCanAccessCurriculaLookup() throws Exception {
        CurriculumLookupOption option = new CurriculumLookupOption(1L, "BSIT-2024", "BS Information Technology", "BSIT", "2024-2025", "ACTIVE", 4);
        given(designerService.getCurriculumLookupOptions()).willReturn(List.of(option));

        mockMvc.perform(get("/api/v1/curricula/lookup")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("ACCOUNTANT can access Scheduling Sections by Term (200 OK)")
    @WithMockUser(username = "accountant_user", roles = {"ACCOUNTANT"})
    void accountantCanAccessSchedulingSections() throws Exception {
        SectionDetailResponse section = new SectionDetailResponse(
                10L, 10L, "1st Sem 2024", 1L, "BSIT", "BS IT",
                101L, "IT101", "Intro to IT", BigDecimal.valueOf(3), BigDecimal.ZERO, BigDecimal.valueOf(3),
                "SEC-A", 40, 35, "OPEN", Collections.emptyList()
        );
        given(schedulingService.getSectionsByTerm(eq(10L), any(), any())).willReturn(List.of(section));

        mockMvc.perform(get("/api/v1/scheduling/sections/term/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("ACCOUNTANT can access Campuses (200 OK)")
    @WithMockUser(username = "accountant_user", roles = {"ACCOUNTANT"})
    void accountantCanAccessCampuses() throws Exception {
        CampusResponse campus = new CampusResponse(1L, "MAIN", "CHED-001", "Main Campus", "Talisay City", "Region VI", "+6334495", "info@chmsu.edu.ph", true, true);
        given(campusService.getAllCampuses()).willReturn(List.of(campus));

        mockMvc.perform(get("/api/v1/campuses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("ACCOUNTANT can access Active Campuses (200 OK)")
    @WithMockUser(username = "accountant_user", roles = {"ACCOUNTANT"})
    void accountantCanAccessActiveCampuses() throws Exception {
        CampusResponse campus = new CampusResponse(1L, "MAIN", "CHED-001", "Main Campus", "Talisay City", "Region VI", "+6334495", "info@chmsu.edu.ph", true, true);
        given(campusService.getActiveCampuses()).willReturn(List.of(campus));

        mockMvc.perform(get("/api/v1/campuses/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("ACCOUNTANT can access Enrollment by Student and Term (200 OK)")
    @WithMockUser(username = "accountant_user", roles = {"ACCOUNTANT"})
    void accountantCanAccessEnrollmentByStudentAndTerm() throws Exception {
        StudentEnrollmentResponse enrollment = new StudentEnrollmentResponse(
                100L, 10L, "2026-00010", 10L, "FIRST_SEM",
                Instant.now(), "ENROLLED", BigDecimal.valueOf(21), false, Collections.emptyList()
        );
        given(enrollmentService.getEnrollment(10L, 10L)).willReturn(enrollment);

        mockMvc.perform(get("/api/v1/enrollment/student/10/term/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(100));
    }

    @Test
    @DisplayName("ACCOUNTANT can access Equity Profiles Search and receives stripped view (200 OK)")
    @WithMockUser(username = "accountant_user", roles = {"ACCOUNTANT"})
    void accountantCanAccessEquityProfilesSearch() throws Exception {
        EquityProfileAccountingView view = new EquityProfileAccountingView(
                10L, "2024-0001", "Juan Dela Cruz", "4PS", "VERIFIED", "BSIT", 10L
        );
        given(equityProfileService.searchForAccounting(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/v1/equity-profiles/search")
                        .param("query", "dela")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].studentId").value(10))
                .andExpect(jsonPath("$.content[0].studentNumber").value("2024-0001"))
                .andExpect(jsonPath("$.content[0].studentName").value("Juan Dela Cruz"))
                .andExpect(jsonPath("$.content[0].equityCategory").value("4PS"))
                .andExpect(jsonPath("$.content[0].verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.content[0].programCode").value("BSIT"))
                .andExpect(jsonPath("$.content[0].evidence").doesNotExist())
                .andExpect(jsonPath("$.content[0].officerRemarks").doesNotExist())
                .andExpect(jsonPath("$.content[0].verificationRemarks").doesNotExist());
    }

    // ─── Parameterized check: None of the 6 endpoints return 403 for ACCOUNTANT ─

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/curricula/lookup",
            "/api/v1/scheduling/sections/term/10",
            "/api/v1/campuses",
            "/api/v1/campuses/active",
            "/api/v1/enrollment/student/10/term/10",
            "/api/v1/equity-profiles/search?query=dela"
    })
    @DisplayName("ACCOUNTANT should not receive 403 on any of the 6 required endpoints")
    @WithMockUser(username = "accountant_user", roles = {"ACCOUNTANT"})
    void accountantShouldNotReceive403OnAnyRequiredEndpoint(String endpoint) throws Exception {
        given(designerService.getCurriculumLookupOptions()).willReturn(Collections.emptyList());
        given(schedulingService.getSectionsByTerm(any(), any(), any())).willReturn(Collections.emptyList());
        given(campusService.getAllCampuses()).willReturn(Collections.emptyList());
        given(campusService.getActiveCampuses()).willReturn(Collections.emptyList());
        given(enrollmentService.getEnrollment(any(), any())).willReturn(
                new StudentEnrollmentResponse(1L, 10L, "2026-00010", 10L, "TERM", Instant.now(), "ENROLLED", BigDecimal.ZERO, false, Collections.emptyList()));
        given(equityProfileService.searchForAccounting(any(), any(), any())).willReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get(endpoint).contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .withFailMessage("Expected status != 403 for " + endpoint + " but got " + status)
                            .isNotEqualTo(403);
                });
    }

    // ─── CASHIER is blocked from equity profile search ───────────────────────

    @Test
    @DisplayName("CASHIER role receives 403 Forbidden on Equity Profiles Search")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierShouldNotAccessEquityProfilesSearch() throws Exception {
        mockMvc.perform(get("/api/v1/equity-profiles/search?query=dela")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ─── Regression: STUDENT still blocked from admin/finance/lookup endpoints ─

    @Test
    @DisplayName("STUDENT role receives 403 Forbidden on curricula lookup")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldNotAccessCurriculaLookup() throws Exception {
        mockMvc.perform(get("/api/v1/curricula/lookup")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT role receives 403 Forbidden on scheduling sections")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldNotAccessSchedulingSections() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT role receives 403 Forbidden on active campuses")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldNotAccessActiveCampuses() throws Exception {
        mockMvc.perform(get("/api/v1/campuses/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT role receives 403 Forbidden on equity profiles search")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldNotAccessEquityProfilesSearch() throws Exception {
        mockMvc.perform(get("/api/v1/equity-profiles/search?query=dela")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ─── Unauthenticated returns 401/403 ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/curricula/lookup",
            "/api/v1/campuses/active",
            "/api/v1/equity-profiles/search?query=dela"
    })
    @DisplayName("Unauthenticated request should receive 401 or 403")
    @WithAnonymousUser
    void unauthenticatedShouldReceiveUnauthorized(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint).contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isIn(401, 403);
                });
    }
}
