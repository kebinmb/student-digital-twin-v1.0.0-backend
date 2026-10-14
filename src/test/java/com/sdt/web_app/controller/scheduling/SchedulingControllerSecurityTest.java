package com.sdt.web_app.controller.scheduling;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.dto.scheduling.SchedulingDtos.FacultyLoadSummaryResponse;
import com.sdt.web_app.service.scheduling.SchedulingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SchedulingControllerSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulingService schedulingService;

    @Test
    @DisplayName("REGISTRAR role can read faculty workload (200 OK)")
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void getFacultyWorkload_Registrar_Success() throws Exception {
        FacultyLoadSummaryResponse dummy = new FacultyLoadSummaryResponse(
                8L, "Dr. Smith", "smith@chmsu.edu.ph", 11L, "AY 2026-2027 - 1st Sem",
                BigDecimal.valueOf(18), BigDecimal.ZERO, BigDecimal.valueOf(18),
                false, null, 2, null, BigDecimal.valueOf(18), null, null,
                Collections.emptyList()
        );
        given(schedulingService.getFacultyWorkload(eq(11L), eq(8L))).willReturn(dummy);

        mockMvc.perform(get("/api/v1/scheduling/faculty-workload/term/11/faculty/8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("STUDENT role is forbidden from reading faculty workload (403 Forbidden)")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void getFacultyWorkload_Student_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/faculty-workload/term/11/faculty/8"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CASHIER role is forbidden from reading faculty workload (403 Forbidden)")
    @WithMockUser(username = "cashier", roles = {"CASHIER"})
    void getFacultyWorkload_Cashier_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/faculty-workload/term/11/faculty/8"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FACULTY role can read faculty workload (200 OK)")
    @WithMockUser(username = "faculty", roles = {"FACULTY"})
    void getFacultyWorkload_Faculty_Success() throws Exception {
        FacultyLoadSummaryResponse dummy = new FacultyLoadSummaryResponse(
                8L, "Dr. Smith", "smith@chmsu.edu.ph", 11L, "AY 2026-2027 - 1st Sem",
                BigDecimal.valueOf(18), BigDecimal.ZERO, BigDecimal.valueOf(18),
                false, null, 2, null, BigDecimal.valueOf(18), null, null,
                Collections.emptyList()
        );
        given(schedulingService.getFacultyWorkload(eq(11L), eq(8L))).willReturn(dummy);

        mockMvc.perform(get("/api/v1/scheduling/faculty-workload/term/11/faculty/8"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("REGISTRAR role can delete class section (204 No Content)")
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void deleteSection_Registrar_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/scheduling/sections/101")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("SUPER_ADMIN role can delete class section (204 No Content)")
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void deleteSection_SuperAdmin_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/scheduling/sections/101")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("FACULTY role is forbidden from deleting class section (403 Forbidden)")
    @WithMockUser(username = "faculty", roles = {"FACULTY"})
    void deleteSection_Faculty_Forbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/scheduling/sections/101")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REGISTRAR role can update term max class hours (200 OK)")
    @WithMockUser(username = "registrar", roles = {"REGISTRAR"})
    void updateTermMaxHours_Registrar_Success() throws Exception {
        com.sdt.web_app.dto.scheduling.SchedulingDtos.SchedulingTermDto dummy =
                new com.sdt.web_app.dto.scheduling.SchedulingDtos.SchedulingTermDto(
                        11L, 1L, "AY-2026", "FIRST_SEMESTER", "AY 2026 - 1st Sem", true, true, true, BigDecimal.valueOf(4.0)
                );
        given(schedulingService.updateTermMaxHoursPerClass(eq(11L), eq(BigDecimal.valueOf(4.0)))).willReturn(dummy);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/scheduling/terms/11/max-class-hours")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"maxHoursPerClass\": 4.0}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DEAN role is forbidden from updating term max class hours (403 Forbidden)")
    @WithMockUser(username = "dean", roles = {"DEAN"})
    void updateTermMaxHours_Dean_Forbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/scheduling/terms/11/max-class-hours")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"maxHoursPerClass\": 4.0}"))
                .andExpect(status().isForbidden());
    }
}
