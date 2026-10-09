package com.sdt.web_app.service.security;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.dto.authentication.UserDtos.UserDetailResponse;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentEnrollmentResponse;
import com.sdt.web_app.dto.financial.FinancialDtos.UnifastFheClaimDto;
import com.sdt.web_app.dto.institution.CampusDtos.CampusResponse;
import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.CurriculumLookupOption;
import com.sdt.web_app.dto.scheduling.SchedulingDtos.SectionDetailResponse;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.service.authentication.UserService;
import com.sdt.web_app.service.enrollment.EnrollmentService;
import com.sdt.web_app.service.financial.UnifastBillingService;
import com.sdt.web_app.service.institution.CampusService;
import com.sdt.web_app.service.institution.CurriculumDesignerService;
import com.sdt.web_app.service.scheduling.SchedulingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CashierEndpointSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurriculumDesignerService designerService;

    @MockitoBean
    private SchedulingService schedulingService;

    @MockitoBean
    private CampusService campusService;

    @MockitoBean
    private UnifastBillingService unifastBillingService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private SecurityUtils securityUtils;

    @MockitoBean
    private StudentProfileRepository studentProfileRepository;

    // ─── All 6 endpoints — CASHIER gets 200 OK ───────────────────────────────

    @Test
    @DisplayName("CASHIER can access Curricula Lookup (200 OK)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierCanAccessCurriculaLookup() throws Exception {
        CurriculumLookupOption option = new CurriculumLookupOption(1L, "BSIT-2024", "BS Information Technology", "BSIT", "2024-2025", "ACTIVE", 4);
        given(designerService.getCurriculumLookupOptions()).willReturn(List.of(option));

        mockMvc.perform(get("/api/v1/curricula/lookup")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("CASHIER can access Scheduling Sections by Term (200 OK)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierCanAccessSchedulingSections() throws Exception {
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
    @DisplayName("CASHIER can access Campuses (200 OK)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierCanAccessCampuses() throws Exception {
        CampusResponse campus = new CampusResponse(1L, "MAIN", "CHED-001", "Main Campus", "Talisay City", "Region VI", "+6334495", "info@chmsu.edu.ph", true, true);
        given(campusService.getAllCampuses()).willReturn(List.of(campus));

        mockMvc.perform(get("/api/v1/campuses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("CASHIER can access UniFAST Claims by Term (200 OK)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierCanAccessUnifastClaims() throws Exception {
        UnifastFheClaimDto claim = new UnifastFheClaimDto(
                1L, "BATCH-2026-01", 10L, "1st Sem 2024", 1L, "Main Campus",
                100, new BigDecimal("500000.00"), new BigDecimal("50000.00"), new BigDecimal("550000.00"),
                "SUBMITTED", "cashier_user", Instant.now().toString(), Collections.emptyList()
        );
        given(unifastBillingService.getClaimsByTerm(10L)).willReturn(List.of(claim));

        mockMvc.perform(get("/api/v1/finance/unifast/claims/term/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("CASHIER can access Users endpoint and receives student-scoped users (200 OK)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierCanAccessUsers() throws Exception {
        UserDetailResponse studentUser = new UserDetailResponse(42L, "student42", "student42@chmsu.edu.ph", Set.of("STUDENT"), true, Instant.now());
        given(userService.getUsersByRole(Roles.STUDENT)).willReturn(List.of(studentUser));

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].roles[0]").value("STUDENT"));
    }

    @Test
    @DisplayName("CASHIER can access Enrollment by Student and Term (200 OK)")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierCanAccessEnrollmentByStudentAndTerm() throws Exception {
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

    // ─── Parameterized check: None of the 6 endpoints return 403 for CASHIER ─

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/curricula/lookup",
            "/api/v1/scheduling/sections/term/10",
            "/api/v1/campuses",
            "/api/v1/finance/unifast/claims/term/10",
            "/api/v1/users",
            "/api/v1/enrollment/student/10/term/10"
    })
    @DisplayName("CASHIER should not receive 403 on any of the 6 required endpoints")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierShouldNotReceive403OnAnyRequiredEndpoint(String endpoint) throws Exception {
        given(designerService.getCurriculumLookupOptions()).willReturn(Collections.emptyList());
        given(schedulingService.getSectionsByTerm(any(), any(), any())).willReturn(Collections.emptyList());
        given(campusService.getAllCampuses()).willReturn(Collections.emptyList());
        given(unifastBillingService.getClaimsByTerm(any())).willReturn(Collections.emptyList());
        given(userService.getUsersByRole(any())).willReturn(Collections.emptyList());
        given(enrollmentService.getEnrollment(any(), any())).willReturn(new StudentEnrollmentResponse(1L, 10L, "2026-00010", 10L, "TERM", Instant.now(), "ENROLLED", BigDecimal.ZERO, false, Collections.emptyList()));

        mockMvc.perform(get(endpoint).contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .withFailMessage("Expected status != 403 for " + endpoint + " but got " + status)
                            .isNotEqualTo(403);
                });
    }

    // ─── CASHIER Data Scoping: Users endpoint scopes out non-students ────────

    @Test
    @DisplayName("CASHIER should only see STUDENT users and not ADMIN or FACULTY accounts")
    @WithMockUser(username = "cashier_user", roles = {"CASHIER"})
    void cashierShouldOnlySeeStudentUsersNotAdminOrFaculty() throws Exception {
        UserDetailResponse student = new UserDetailResponse(50L, "student_juan", "juan@chmsu.edu.ph", Set.of("STUDENT"), true, Instant.now());
        given(userService.getUsersByRole(Roles.STUDENT)).willReturn(List.of(student));

        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("student_juan"))
                .andExpect(jsonPath("$[?(@.roles[0] == 'ADMIN')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.roles[0] == 'FACULTY')]").doesNotExist());
    }

    // ─── Regression: STUDENT still blocked from admin/finance endpoints ──────

    @Test
    @DisplayName("STUDENT role receives 403 Forbidden on UniFAST claims")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldNotAccessUnifastClaims() throws Exception {
        mockMvc.perform(get("/api/v1/finance/unifast/claims/term/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STUDENT role receives 403 Forbidden on all users endpoint")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void studentShouldNotAccessAllUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ─── Unauthenticated returns 401/403 ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/curricula/lookup",
            "/api/v1/campuses",
            "/api/v1/finance/unifast/claims/term/10"
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
