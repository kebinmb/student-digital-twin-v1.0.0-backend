package com.sdt.web_app.controller.scheduling;

import com.sdt.web_app.service.scheduling.SchedulingService;
import com.sdt.web_app.service.scheduling.SectionEventPublisherService;
import com.sdt.web_app.service.security.AcademicScopeAssertionService;
import com.sdt.web_app.service.security.DataScopingService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SchedulingControllerStreamTest {

    @Mock
    private SchedulingService schedulingService;

    @Mock
    private SectionEventPublisherService sectionEventPublisherService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private DataScopingService dataScopingService;

    @Mock
    private AcademicScopeAssertionService academicScopeAssertionService;

    @InjectMocks
    private SchedulingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/scheduling/sections/stream subscribes to term section events SSE stream")
    void streamSectionEvents_ReturnsSseEmitter() throws Exception {
        Long termId = 12L;
        SseEmitter mockEmitter = new SseEmitter(60000L);
        given(sectionEventPublisherService.subscribeToTermSectionEvents(eq(termId))).willReturn(mockEmitter);

        MvcResult result = mockMvc.perform(get("/api/v1/scheduling/sections/stream")
                        .param("termId", termId.toString())
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
