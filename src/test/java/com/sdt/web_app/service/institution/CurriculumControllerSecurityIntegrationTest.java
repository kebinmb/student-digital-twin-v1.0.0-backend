package com.sdt.web_app.service.institution;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.DepartmentType;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CurriculumControllerSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CampusRepository campusRepository;

    private Curriculum draftCurriculum;
    private Curriculum activeCurriculum;

    @BeforeEach
    void setUp() {
        Campus campus = campusRepository.save(Campus.builder()
                .code("CAMPUS-SEC-CURR")
                .name("Campus Sec Curr")
                .region("Region VI")
                .build());

        Department dept = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("DEPT-SEC-CURR")
                .name("Dept Sec Curr")
                .type(DepartmentType.COLLEGE)
                .build());

        Program prog = programRepository.save(Program.builder()
                .department(dept)
                .code("PROG-SEC-CURR")
                .name("Program Sec Curr")
                .degreeLevel("UNDERGRADUATE")
                .totalUnitsRequired(140)
                .build());

        draftCurriculum = curriculumRepository.save(Curriculum.builder()
                .program(prog)
                .code("CURR-SEC-DRAFT")
                .name("Draft Curriculum")
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.DRAFT)
                .versionNumber(1)
                .build());

        activeCurriculum = curriculumRepository.save(Curriculum.builder()
                .program(prog)
                .code("CURR-SEC-ACTIVE")
                .name("Active Curriculum")
                .effectiveAcademicYear("2025-2026")
                .status(Curriculum.Status.ACTIVE)
                .versionNumber(1)
                .build());
    }

    @Test
    @DisplayName("ADMIN role should successfully delete draft curriculum (204 No Content)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteCurriculum_Admin_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/curricula/" + draftCurriculum.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DEAN role should successfully delete draft curriculum (204 No Content)")
    @WithMockUser(username = "dean", roles = {"DEAN"})
    void deleteCurriculum_Dean_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/curricula/" + draftCurriculum.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Attempting to delete an ACTIVE curriculum should return 409 Conflict")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteCurriculum_Active_Conflict() throws Exception {
        mockMvc.perform(delete("/api/v1/curricula/" + activeCurriculum.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("STUDENT role should be forbidden from deleting curriculum (403 Forbidden)")
    @WithMockUser(username = "student", roles = {"STUDENT"})
    void deleteCurriculum_Student_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/curricula/" + draftCurriculum.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FACULTY role should be forbidden from deleting curriculum (403 Forbidden)")
    @WithMockUser(username = "faculty", roles = {"FACULTY"})
    void deleteCurriculum_Faculty_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/curricula/" + draftCurriculum.getId()))
                .andExpect(status().isForbidden());
    }
}
