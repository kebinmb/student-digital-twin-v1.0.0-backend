package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.AttendanceSessionResponse;
import com.sdt.web_app.dto.analytics.AnalyticsDtos.StartAttendanceSessionRequest;
import com.sdt.web_app.service.analytics.QrAttendanceService;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock
    private QrAttendanceService attendanceService;

    @Mock
    private StudentService studentService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AttendanceController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/attendance/session/start generates dynamic QR attendance session")
    void startSession_ReturnsCreatedSession() throws Exception {
        StartAttendanceSessionRequest request = new StartAttendanceSessionRequest(
                10L,
                new BigDecimal("10.6385"),
                new BigDecimal("122.9723"),
                50
        );

        AttendanceSessionResponse mockResponse = new AttendanceSessionResponse(
                101L,
                10L,
                "QR-ATT-SESSION-123",
                Instant.now().plusSeconds(900),
                new BigDecimal("10.6385"),
                new BigDecimal("122.9723"),
                50,
                "data:image/png;base64,mockQrBarcode"
        );

        when(securityUtils.resolveUserId(any())).thenReturn(25L);
        when(attendanceService.startSession(any(StartAttendanceSessionRequest.class), eq(25L))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/attendance/session/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(101L))
                .andExpect(jsonPath("$.sectionScheduleId").value(10L))
                .andExpect(jsonPath("$.qrSeed").value("QR-ATT-SESSION-123"))
                .andExpect(jsonPath("$.qrCodeDataUrl").value("data:image/png;base64,mockQrBarcode"));
    }
}
