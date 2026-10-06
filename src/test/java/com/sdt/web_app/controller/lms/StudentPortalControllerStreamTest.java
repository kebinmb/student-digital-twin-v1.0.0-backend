package com.sdt.web_app.controller.lms;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.service.lms.StudentNotificationPublisherService;
import com.sdt.web_app.service.lms.StudentPortalService;
import com.sdt.web_app.service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentPortalControllerStreamTest {

    @Mock
    private StudentPortalService portalService;

    @Mock
    private StudentNotificationPublisherService notificationService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private StudentPortalController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/students/portal/stream returns active SSE stream for specified student")
    void streamStudentNotifications_ReturnsSseEmitter() throws Exception {
        Long studentId = 77L;
        SseEmitter mockEmitter = new SseEmitter(60000L);
        given(notificationService.subscribeToStudentEvents(eq(studentId))).willReturn(mockEmitter);

        MvcResult result = mockMvc.perform(get("/api/v1/students/portal/stream")
                        .param("studentId", studentId.toString())
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
