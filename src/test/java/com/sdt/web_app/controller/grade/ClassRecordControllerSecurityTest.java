package com.sdt.web_app.controller.grade;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.dto.grade.ClassRecordDtos.ClassRecordItemDto;
import com.sdt.web_app.dto.grade.ClassRecordDtos.ClassRecordMatrixResponse;
import com.sdt.web_app.dto.grade.ClassRecordDtos.CreateClassRecordItemRequest;
import com.sdt.web_app.dto.grade.ClassRecordDtos.SectionGradingConfigResponse;
import com.sdt.web_app.service.grade.ClassRecordService;
import com.sdt.web_app.service.scheduling.SectionSecurity;
import com.sdt.web_app.service.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClassRecordControllerSecurityTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassRecordService classRecordService;

    @MockitoBean
    private SectionSecurity sectionSecurity;

    @MockitoBean
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("FACULTY role can add assessment item for their section (201 Created)")
    @WithMockUser(username = "faculty_user", roles = {"FACULTY"})
    void faculty_CanAddAssessmentItem() throws Exception {
        Long sectionId = 12L;
        given(sectionSecurity.canAccessSection(eq(sectionId), any())).willReturn(true);
        given(securityUtils.resolveUserId(any())).willReturn(5L);

        ClassRecordItemDto itemDto = new ClassRecordItemDto(101L, 1L, "Quiz 1", new BigDecimal("50.00"), 1);
        given(classRecordService.addAssessmentItem(eq(sectionId), any(CreateClassRecordItemRequest.class), eq(5L)))
                .willReturn(itemDto);

        String payload = """
                {
                    "categoryId": 1,
                    "itemTitle": "Quiz 1",
                    "maxPoints": 50.00,
                    "sequenceOrder": 1
                }
                """;

        mockMvc.perform(post("/api/v1/class-records/sections/" + sectionId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.itemTitle").value("Quiz 1"));
    }

    @Test
    @DisplayName("Duplicate assessment item throws 409 Conflict")
    @WithMockUser(username = "faculty_user", roles = {"FACULTY"})
    void duplicateAssessmentItem_Returns409Conflict() throws Exception {
        Long sectionId = 12L;
        given(sectionSecurity.canAccessSection(eq(sectionId), any())).willReturn(true);
        given(securityUtils.resolveUserId(any())).willReturn(5L);

        given(classRecordService.addAssessmentItem(eq(sectionId), any(CreateClassRecordItemRequest.class), eq(5L)))
                .willThrow(new IllegalStateException("An assessment item with title 'Quiz 1' already exists in this category"));

        String payload = """
                {
                    "categoryId": 1,
                    "itemTitle": "Quiz 1",
                    "maxPoints": 50.00,
                    "sequenceOrder": 1
                }
                """;

        mockMvc.perform(post("/api/v1/class-records/sections/" + sectionId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An assessment item with title 'Quiz 1' already exists in this category"));
    }

    @Test
    @DisplayName("FACULTY role can get score matrix for dynamic section ID (200 OK)")
    @WithMockUser(username = "faculty_user", roles = {"FACULTY"})
    void faculty_CanGetScoreMatrix_DynamicSection() throws Exception {
        Long sectionId = 42L;
        given(sectionSecurity.canAccessSection(eq(sectionId), any())).willReturn(true);

        SectionGradingConfigResponse configResponse = new SectionGradingConfigResponse(
                1L, sectionId, new BigDecimal("50.00"), new BigDecimal("50.00"), false, Collections.emptyList()
        );
        ClassRecordMatrixResponse matrixResponse = new ClassRecordMatrixResponse(
                sectionId, "BSIT-3A", "IT 312", "Database Systems", configResponse, Collections.emptyList()
        );

        given(classRecordService.getScoreMatrix(eq(sectionId))).willReturn(matrixResponse);

        mockMvc.perform(get("/api/v1/class-records/sections/" + sectionId + "/matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionId").value(42))
                .andExpect(jsonPath("$.sectionCode").value("BSIT-3A"));
    }

    @Test
    @DisplayName("REGISTRAR role cannot add assessment item (403 Forbidden)")
    @WithMockUser(username = "registrar_user", roles = {"REGISTRAR"})
    void registrar_CannotAddAssessmentItem() throws Exception {
        Long sectionId = 12L;
        given(sectionSecurity.canAccessSection(eq(sectionId), any())).willReturn(true);

        String payload = """
                {
                    "categoryId": 1,
                    "itemTitle": "Quiz 1",
                    "maxPoints": 50.00,
                    "sequenceOrder": 1
                }
                """;

        mockMvc.perform(post("/api/v1/class-records/sections/" + sectionId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Access denied when SectionSecurity rejects section access (403 Forbidden)")
    @WithMockUser(username = "faculty_other", roles = {"FACULTY"})
    void faculty_ForbiddenWhenCannotAccessSection() throws Exception {
        Long sectionId = 12L;
        given(sectionSecurity.canAccessSection(eq(sectionId), any())).willReturn(false);

        mockMvc.perform(get("/api/v1/class-records/sections/" + sectionId + "/matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request is rejected (401 or 403)")
    void unauthenticated_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/class-records/sections/12/matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }
}
