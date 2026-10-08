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
}
