package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.DigitalTwinRiskProfileDto;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentProfileResponse;
import com.sdt.web_app.service.analytics.DigitalTwinRiskService;
import com.sdt.web_app.service.enrollment.StudentService;
import com.sdt.web_app.service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DigitalTwinAnalyticsControllerTest {

    @Mock
    private DigitalTwinRiskService riskService;

    @Mock
    private StudentService studentService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DigitalTwinAnalyticsController controller;

    private MockMvc mockMvc;

    private DigitalTwinRiskProfileDto mockProfile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockProfile = new DigitalTwinRiskProfileDto(
                10L,
                "2026-0001",
                "Alice Student",
                "BSIT",
                3,
                new BigDecimal("82.50"),
                new BigDecimal("75.00"),
                new BigDecimal("20.00"),
                "MODERATE",
                new BigDecimal("0.2500"),
                List.of("Peer Tutoring"),
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/analytics/digital-twin/risk/me routes correctly without converting 'me' to numeric studentId")
    void getCurrentStudentRiskProfile_ResolvesCurrentStudent() throws Exception {
        when(securityUtils.resolveUserId(any())).thenReturn(100L);

        StudentProfileResponse profileResponse = new StudentProfileResponse(
                10L, "2026-0001", 100L, "alice", "alice@example.com",
                1L, "BSIT", "BS Information Technology",
                1L, "BSIT-2024", "REGULAR", 3,
                "ENROLLED", true, new BigDecimal("60.00"), new BigDecimal("1.75"), "CLEARED", "CLEARED"
        );
        when(studentService.getStudentByUserId(100L)).thenReturn(profileResponse);
        when(riskService.evaluateStudentRiskProfile(10L)).thenReturn(mockProfile);

        mockMvc.perform(get("/api/v1/analytics/digital-twin/risk/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(10))
                .andExpect(jsonPath("$.studentName").value("Alice Student"))
                .andExpect(jsonPath("$.compositeRiskLevel").value("MODERATE"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/digital-twin/risk/{studentId} routes correctly for numeric ID")
    void getStudentRiskProfile_ResolvesNumericStudentId() throws Exception {
        when(riskService.evaluateStudentRiskProfile(10L)).thenReturn(mockProfile);

        mockMvc.perform(get("/api/v1/analytics/digital-twin/risk/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(10))
                .andExpect(jsonPath("$.studentNumber").value("2026-0001"));
    }
}
