package com.sdt.web_app.controller.compliance;

import com.sdt.web_app.dto.compliance.EquityDtos.ApplicantEquityStatsDto;
import com.sdt.web_app.service.compliance.StudentEquityProfileService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudentEquityProfileControllerTest {

    @Mock
    private StudentEquityProfileService equityProfileService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private StudentEquityProfileController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/equity-profiles/admission-applicants/statistics returns 200 OK with statistics")
    void getAdmissionApplicantEquityStats_Success() throws Exception {
        ApplicantEquityStatsDto stats = ApplicantEquityStatsDto.builder()
                .totalPostExamCount(42L)
                .examPassedCount(38L)
                .examFailedCount(4L)
                .count4psBeneficiaries(14L)
                .countIndigenousPeoples(5L)
                .countPersonsWithDisabilities(3L)
                .countSoloParents(7L)
                .countOrphans(2L)
                .countGidaResidents(9L)
                .countFarmerFisherfolk(11L)
                .countBottom40IncomeBracket(28L)
                .countFirstGenerationCollege(20L)
                .build();

        when(equityProfileService.getPostExamApplicantEquityStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/equity-profiles/admission-applicants/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPostExamCount").value(42))
                .andExpect(jsonPath("$.examPassedCount").value(38))
                .andExpect(jsonPath("$.count4psBeneficiaries").value(14))
                .andExpect(jsonPath("$.countIndigenousPeoples").value(5))
                .andExpect(jsonPath("$.countPersonsWithDisabilities").value(3))
                .andExpect(jsonPath("$.countBottom40IncomeBracket").value(28));
    }

    @Test
    @DisplayName("GET /api/v1/compliance/equity-profiles/admission-applicants/statistics supports alternate mapped path")
    void getAdmissionApplicantEquityStats_AlternativePath_Success() throws Exception {
        ApplicantEquityStatsDto stats = ApplicantEquityStatsDto.builder()
                .totalPostExamCount(10L)
                .examPassedCount(10L)
                .build();

        when(equityProfileService.getPostExamApplicantEquityStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/compliance/equity-profiles/admission-applicants/statistics")
                        .param("cohort", "ADMISSION")
                        .param("termId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPostExamCount").value(10))
                .andExpect(jsonPath("$.examPassedCount").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/equity-profiles/admission-applicants/statistics returns 200 OK with zero defaults when no records exist")
    void getAdmissionApplicantEquityStats_EmptyDataset_ReturnsZeros() throws Exception {
        ApplicantEquityStatsDto emptyStats = ApplicantEquityStatsDto.builder()
                .totalPostExamCount(0L)
                .examPassedCount(0L)
                .examFailedCount(0L)
                .count4psBeneficiaries(0L)
                .countIndigenousPeoples(0L)
                .countPersonsWithDisabilities(0L)
                .countSoloParents(0L)
                .countOrphans(0L)
                .countGidaResidents(0L)
                .countFarmerFisherfolk(0L)
                .countBottom40IncomeBracket(0L)
                .countFirstGenerationCollege(0L)
                .build();

        when(equityProfileService.getPostExamApplicantEquityStatistics()).thenReturn(emptyStats);

        mockMvc.perform(get("/api/v1/equity-profiles/admission-applicants/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPostExamCount").value(0))
                .andExpect(jsonPath("$.examPassedCount").value(0))
                .andExpect(jsonPath("$.count4psBeneficiaries").value(0));
    }
}
