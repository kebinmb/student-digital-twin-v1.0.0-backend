package com.sdt.web_app.service.institution;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sdt.web_app.controller.TestSecurityControllers;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.*;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestSecurityControllers.class)
class CleanSlateSchemaAndEmptyDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ClassSectionRepository sectionRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private FacultyProfileRepository facultyProfileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;



    @Test
    @DisplayName("Verify ghost table role_permissions is dropped and term_name column is removed")
    void verifyGhostTableAndOrphanColumnRemoved() {
        // 1. Verify role_permissions table does not exist
        Integer rolePermTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'role_permissions'",
                Integer.class);
        assertThat(rolePermTableCount).isZero();

        // 2. Verify terms.term_name column does not exist
        Integer termNameColCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'terms' AND column_name = 'term_name'",
                Integer.class);
        assertThat(termNameColCount).isZero();
    }

    @Test
    @DisplayName("Verify single root administrator account is the sole user in database with unrestricted scope")
    void verifySoleRootAdminUser() {
        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(1);

        User admin = allUsers.get(0);
        assertThat(admin.getId()).isEqualTo(1L);
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.getCollege()).isNull();
        assertThat(admin.getProgram()).isNull();
        assertThat(admin.getRoles()).containsExactly(Roles.ADMIN);
    }

    @Test
    @DisplayName("Verify all domain and operational tables are completely empty")
    void verifyDomainTablesEmpty() {
        assertThat(campusRepository.count()).isZero();
        assertThat(departmentRepository.count()).isZero();
        assertThat(programRepository.count()).isZero();
        assertThat(curriculumRepository.count()).isZero();
        assertThat(courseRepository.count()).isZero();
        assertThat(sectionRepository.count()).isZero();
        assertThat(studentProfileRepository.count()).isZero();
        assertThat(facultyProfileRepository.count()).isZero();
    }

    @Test
    @DisplayName("Verify core GET endpoints return HTTP 200 with empty lists ([])")
    void verifyEmptyEndpointsReturnEmptyLists() throws Exception {
        // Authenticate admin first to get JWT token
        String loginPayload = """
                {
                    "usernameOrEmail": "admin",
                    "password": "Password123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String token = jsonNode.get("accessToken").asText();

        // 1. GET /api/v1/departments -> []
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // 2. GET /api/v1/programs -> []
        mockMvc.perform(get("/api/v1/programs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // 3. GET /api/v1/students/search -> []
        mockMvc.perform(get("/api/v1/students/search")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Verify POST /api/public/auth/login succeeds with username alias and claims have null scoping")
    void verifyAdminLoginAndClaims() throws Exception {
        String loginPayload = """
                {
                    "username": "admin",
                    "password": "Password123!"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String token = jsonNode.get("accessToken").asText();

        // Decode JWT payload
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        String claimsJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode claims = objectMapper.readTree(claimsJson);

        assertThat(claims.get("preferred_username").asText()).isEqualTo("admin");
        assertThat(claims.has("college_id")).isFalse();
        assertThat(claims.has("program_id")).isFalse();
        assertThat(claims.get("roles").toString()).contains("ADMIN");
    }
}
