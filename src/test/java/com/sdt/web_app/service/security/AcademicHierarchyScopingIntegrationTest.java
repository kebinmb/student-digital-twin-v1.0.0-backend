package com.sdt.web_app.service.security;

import tools.jackson.databind.ObjectMapper;
import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.dto.institution.CurriculumDtos.CreateCurriculumRequest;
import com.sdt.web_app.dto.scheduling.SchedulingDtos.CreateSectionRequest;
import com.sdt.web_app.dto.scheduling.SchedulingDtos.ScheduleSlotDto;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.entities.scheduling.Room;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.*;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.repositories.scheduling.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class AcademicHierarchyScopingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private CurriculumCourseRepository curriculumCourseRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ClassSectionRepository sectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacultyProfileRepository facultyProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private EnrollmentCourseItemRepository enrollmentCourseItemRepository;

    private Department collegeA;
    private Department collegeB;
    private Program progA1;
    private Program progB1;
    private Curriculum currA1;
    private Curriculum currB1;
    private Course courseA1;
    private Course courseB1;
    private Term term;
    private Room roomA;
    private ClassSection sectionA1;
    private ClassSection sectionB1;
    private StudentProfile studentA1;
    private StudentProfile studentB1;

    @BeforeEach
    void setUp() {
        Campus campus = campusRepository.save(Campus.builder()
                .code("CAMPUS-SCOPE")
                .name("Scoping Test Campus")
                .region("Region VI")
                .build());

        // Colleges
        collegeA = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("CCS-SCOPE")
                .name("College of Computer Studies")
                .type(DepartmentType.COLLEGE)
                .build());

        collegeB = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("COE-SCOPE")
                .name("College of Engineering")
                .type(DepartmentType.COLLEGE)
                .build());

        Department deptA1 = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("IT-DEPT")
                .name("Information Technology Dept")
                .type(DepartmentType.DEPARTMENT)
                .parentDepartment(collegeA)
                .build());

        Department deptB1 = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("CE-DEPT")
                .name("Civil Engineering Dept")
                .type(DepartmentType.DEPARTMENT)
                .parentDepartment(collegeB)
                .build());

        // Programs
        progA1 = programRepository.save(Program.builder()
                .department(deptA1)
                .college(collegeA)
                .code("BSIT-SCOPE")
                .name("BS in Information Technology")
                .degreeLevel("UNDERGRADUATE")
                .totalUnitsRequired(140)
                .build());

        progB1 = programRepository.save(Program.builder()
                .department(deptB1)
                .college(collegeB)
                .code("BSCE-SCOPE")
                .name("BS in Civil Engineering")
                .degreeLevel("UNDERGRADUATE")
                .totalUnitsRequired(150)
                .build());

        // Academic Period
        AcademicYear ay = academicYearRepository.save(AcademicYear.builder()
                .code("AY-2026-2027-SCOPE")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2027, 5, 30))
                .isCurrent(true)
                .build());

        term = termRepository.save(Term.builder()
                .academicYear(ay)
                .termType(TermType.FIRST_SEM)
                .startDate(LocalDate.of(2026, 8, 15))
                .endDate(LocalDate.of(2026, 12, 20))
                .isActive(true)
                .maxHoursPerClass(new BigDecimal("3.0"))
                .build());

        roomA = roomRepository.save(Room.builder()
                .campus(campus)
                .code("RM-SCOPE-101")
                .name("Scope Lecture Room")
                .building("Tech Wing")
                .capacity(50)
                .roomType(Room.RoomType.LECTURE)
                .isActive(true)
                .build());

        // Curricula & Courses
        currA1 = curriculumRepository.save(Curriculum.builder()
                .program(progA1)
                .code("CURR-BSIT-2026")
                .name("BSIT Curriculum 2026")
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.ACTIVE)
                .versionNumber(1)
                .build());

        currB1 = curriculumRepository.save(Curriculum.builder()
                .program(progB1)
                .code("CURR-BSCE-2026")
                .name("BSCE Curriculum 2026")
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.ACTIVE)
                .versionNumber(1)
                .build());

        courseA1 = courseRepository.save(Course.builder()
                .code("IT101-SCOPE")
                .title("Intro to Computing")
                .lectureUnits(new BigDecimal("3.0"))
                .labUnits(BigDecimal.ZERO)
                .creditUnits(new BigDecimal("3.0"))
                .build());

        courseB1 = courseRepository.save(Course.builder()
                .code("CE101-SCOPE")
                .title("Civil Engineering Orientation")
                .lectureUnits(new BigDecimal("3.0"))
                .labUnits(BigDecimal.ZERO)
                .creditUnits(new BigDecimal("3.0"))
                .build());

        curriculumCourseRepository.save(CurriculumCourse.builder()
                .curriculum(currA1)
                .course(courseA1)
                .yearLevel(1)
                .semester("1ST_SEM")
                .build());

        curriculumCourseRepository.save(CurriculumCourse.builder()
                .curriculum(currB1)
                .course(courseB1)
                .yearLevel(1)
                .semester("1ST_SEM")
                .build());

        // Users & Roles
        // DEAN A
        User deanUser = userRepository.save(User.builder()
                .username("dean_ccs")
                .email("dean_ccs@test.com")
                .password("hash")
                .enabled(true)
                .college(collegeA)
                .roles(Set.of(Roles.DEAN))
                .build());

        // DEAN Unlinked
        userRepository.save(User.builder()
                .username("dean_unlinked")
                .email("dean_unlinked@test.com")
                .password("hash")
                .enabled(true)
                .college(null)
                .roles(Set.of(Roles.DEAN))
                .build());

        // CHAIRPERSON A1
        userRepository.save(User.builder()
                .username("chair_it")
                .email("chair_it@test.com")
                .password("hash")
                .enabled(true)
                .college(collegeA)
                .program(progA1)
                .roles(Set.of(Roles.CHAIRPERSON))
                .build());

        // CHAIRPERSON Unlinked
        userRepository.save(User.builder()
                .username("chair_unlinked")
                .email("chair_unlinked@test.com")
                .password("hash")
                .enabled(true)
                .college(null)
                .program(null)
                .roles(Set.of(Roles.CHAIRPERSON))
                .build());

        // CHAIRPERSON Mismatched
        userRepository.save(User.builder()
                .username("chair_mismatched")
                .email("chair_mismatched@test.com")
                .password("hash")
                .enabled(true)
                .college(collegeB)
                .program(progA1)
                .roles(Set.of(Roles.CHAIRPERSON))
                .build());

        // FACULTY A1
        User facultyUser = userRepository.save(User.builder()
                .username("faculty_john")
                .email("faculty_john@test.com")
                .password("hash")
                .enabled(true)
                .college(collegeA)
                .program(progA1)
                .roles(Set.of(Roles.FACULTY))
                .build());

        facultyProfileRepository.save(FacultyProfile.builder()
                .user(facultyUser)
                .college(collegeA)
                .program(progA1)
                .facultyIdNumber("FAC-001")
                .build());

        // FACULTY Other (Unassigned)
        userRepository.save(User.builder()
                .username("faculty_other")
                .email("faculty_other@test.com")
                .password("hash")
                .enabled(true)
                .college(collegeA)
                .program(progA1)
                .roles(Set.of(Roles.FACULTY))
                .build());

        // Students
        User studentUserA = userRepository.save(User.builder()
                .username("student_alice")
                .email("alice@test.com")
                .password("hash")
                .enabled(true)
                .college(collegeA)
                .program(progA1)
                .roles(Set.of(Roles.STUDENT))
                .build());

        studentA1 = studentProfileRepository.save(StudentProfile.builder()
                .user(studentUserA)
                .studentNumber("STUD-CCS-001")
                .program(progA1)
                .curriculum(currA1)
                .build());

        User studentUserB = userRepository.save(User.builder()
                .username("student_bob")
                .email("bob@test.com")
                .password("hash")
                .enabled(true)
                .college(collegeB)
                .program(progB1)
                .roles(Set.of(Roles.STUDENT))
                .build());

        studentB1 = studentProfileRepository.save(StudentProfile.builder()
                .user(studentUserB)
                .studentNumber("STUD-COE-001")
                .program(progB1)
                .curriculum(currB1)
                .build());

        // Sections
        sectionA1 = sectionRepository.save(ClassSection.builder()
                .term(term)
                .curriculum(currA1)
                .course(courseA1)
                .sectionCode("BSIT-1A")
                .primaryInstructor(facultyUser)
                .build());

        sectionB1 = sectionRepository.save(ClassSection.builder()
                .term(term)
                .curriculum(currB1)
                .course(courseB1)
                .sectionCode("BSCE-1A")
                .build());

        // Enlist studentA1 into sectionA1
        StudentEnrollment enrollmentA1 = studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(studentA1)
                .term(term)
                .status(StudentEnrollment.Status.ENROLLED)
                .build());

        enrollmentCourseItemRepository.save(EnrollmentCourseItem.builder()
                .enrollment(enrollmentA1)
                .section(sectionA1)
                .build());

        // Enlist studentB1 into sectionB1
        StudentEnrollment enrollmentB1 = studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(studentB1)
                .term(term)
                .status(StudentEnrollment.Status.ENROLLED)
                .build());

        enrollmentCourseItemRepository.save(EnrollmentCourseItem.builder()
                .enrollment(enrollmentB1)
                .section(sectionB1)
                .build());
    }

    // =========================================================================
    // 1. DEAN ROLE TESTS
    // =========================================================================

    @Test
    @DisplayName("DEAN: Can view sections under assigned college (200 OK)")
    @WithMockUser(username = "dean_ccs", roles = {"DEAN"})
    void dean_CanAccessAllowedCollegeSection_Success() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/" + sectionA1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionCode").value("BSIT-1A"));
    }

    @Test
    @DisplayName("DEAN: Cannot view sections belonging to another college (403 Forbidden)")
    @WithMockUser(username = "dean_ccs", roles = {"DEAN"})
    void dean_CannotAccessOtherCollegeSection_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/" + sectionB1.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DEAN: Querying sections by term only returns sections under Dean's college")
    @WithMockUser(username = "dean_ccs", roles = {"DEAN"})
    void dean_SectionsByTerm_FiltersToCollegeScope() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/" + term.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sectionCode").value("BSIT-1A"));
    }

    @Test
    @DisplayName("DEAN: Can view student profile within assigned college (200 OK)")
    @WithMockUser(username = "dean_ccs", roles = {"DEAN"})
    void dean_CanAccessAllowedCollegeStudent_Success() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + studentA1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").value("STUD-CCS-001"));
    }

    @Test
    @DisplayName("DEAN: Cannot view student enrolled in another college (403 Forbidden)")
    @WithMockUser(username = "dean_ccs", roles = {"DEAN"})
    void dean_CannotAccessOtherCollegeStudent_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + studentB1.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DEAN: Cannot create curriculum in another college (403 Forbidden)")
    @WithMockUser(username = "dean_ccs", roles = {"DEAN"})
    void dean_CannotMutateOtherCollegeProgram_Forbidden() throws Exception {
        CreateCurriculumRequest req = new CreateCurriculumRequest(
                progB1.getId(),
                "CURR-MUTATE-FAIL",
                "Unauthorized Cross-College Curriculum",
                "2026-2027"
        );

        mockMvc.perform(post("/api/v1/curricula")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DEAN: Unlinked Dean account is rejected (403 Forbidden)")
    @WithMockUser(username = "dean_unlinked", roles = {"DEAN"})
    void dean_UnlinkedAccount_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/" + term.getId()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. CHAIRPERSON ROLE TESTS
    // =========================================================================

    @Test
    @DisplayName("CHAIRPERSON: Can view sections under assigned program (200 OK)")
    @WithMockUser(username = "chair_it", roles = {"CHAIRPERSON"})
    void chairperson_CanAccessAllowedProgramSection_Success() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/" + sectionA1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionCode").value("BSIT-1A"));
    }

    @Test
    @DisplayName("CHAIRPERSON: Cannot view sections outside assigned program (403 Forbidden)")
    @WithMockUser(username = "chair_it", roles = {"CHAIRPERSON"})
    void chairperson_CannotAccessOtherProgramSection_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/" + sectionB1.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CHAIRPERSON: Querying sections by term only returns sections under assigned program")
    @WithMockUser(username = "chair_it", roles = {"CHAIRPERSON"})
    void chairperson_SectionsByTerm_FiltersToProgramScope() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/" + term.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sectionCode").value("BSIT-1A"));
    }

    @Test
    @DisplayName("CHAIRPERSON: Can view student enrolled in assigned program (200 OK)")
    @WithMockUser(username = "chair_it", roles = {"CHAIRPERSON"})
    void chairperson_CanAccessAllowedProgramStudent_Success() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + studentA1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNumber").value("STUD-CCS-001"));
    }

    @Test
    @DisplayName("CHAIRPERSON: Cannot view student enrolled in other program (403 Forbidden)")
    @WithMockUser(username = "chair_it", roles = {"CHAIRPERSON"})
    void chairperson_CannotAccessOtherProgramStudent_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + studentB1.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CHAIRPERSON: Cannot create curriculum in another program (403 Forbidden)")
    @WithMockUser(username = "chair_it", roles = {"CHAIRPERSON"})
    void chairperson_CannotMutateOtherProgramCurriculum_Forbidden() throws Exception {
        CreateCurriculumRequest req = new CreateCurriculumRequest(
                progB1.getId(),
                "CURR-CHAIR-FAIL",
                "Unauthorized Cross-Program Curriculum",
                "2026-2027"
        );

        mockMvc.perform(post("/api/v1/curricula")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CHAIRPERSON: Unlinked account is rejected (403 Forbidden)")
    @WithMockUser(username = "chair_unlinked", roles = {"CHAIRPERSON"})
    void chairperson_UnlinkedAccount_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/" + term.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CHAIRPERSON: Mismatched College and Program is rejected (403 Forbidden)")
    @WithMockUser(username = "chair_mismatched", roles = {"CHAIRPERSON"})
    void chairperson_MismatchedScope_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/" + term.getId()))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. FACULTY ROLE TESTS
    // =========================================================================

    @Test
    @DisplayName("FACULTY: Can view gradebook roster for assigned class section (200 OK)")
    @WithMockUser(username = "faculty_john", roles = {"FACULTY"})
    void faculty_CanAccessAssignedSectionRoster_Success() throws Exception {
        mockMvc.perform(get("/api/v1/sections/" + sectionA1.getId() + "/roster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionCode").value("BSIT-1A"));
    }

    @Test
    @DisplayName("FACULTY: Cannot view gradebook roster for unassigned section (403 Forbidden)")
    @WithMockUser(username = "faculty_john", roles = {"FACULTY"})
    void faculty_CannotAccessUnassignedSectionRoster_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/sections/" + sectionB1.getId() + "/roster"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FACULTY: Other unassigned faculty cannot view section roster (403 Forbidden)")
    @WithMockUser(username = "faculty_other", roles = {"FACULTY"})
    void faculty_OtherFacultyCannotAccessRoster_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/sections/" + sectionA1.getId() + "/roster"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FACULTY: Querying sections by term only returns assigned teaching load")
    @WithMockUser(username = "faculty_john", roles = {"FACULTY"})
    void faculty_SectionsByTerm_ReturnsOnlyAssignedSections() throws Exception {
        mockMvc.perform(get("/api/v1/scheduling/sections/term/" + term.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sectionCode").value("BSIT-1A"));
    }

    @Test
    @DisplayName("FACULTY: Searching students only returns students in faculty's assigned sections")
    @WithMockUser(username = "faculty_john", roles = {"FACULTY"})
    void faculty_SearchStudents_ReturnsOnlyEnrolledStudentsInAssignedSections() throws Exception {
        mockMvc.perform(get("/api/v1/students/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentIdNumber").value("STUD-CCS-001"));
    }

    @Test
    @DisplayName("ADMIN: Can update faculty account, syncing college and program without cycle or crash (200 OK)")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void admin_UpdateFacultyUserAccount_SuccessAndSynchronized() throws Exception {
        User faculty = userRepository.findByUsername("faculty_john").orElseThrow();
        com.sdt.web_app.dto.authentication.UserDtos.UpdateUserRequest updateReq =
                new com.sdt.web_app.dto.authentication.UserDtos.UpdateUserRequest(
                        "faculty.john.updated@test.com",
                        null,
                        Set.of("FACULTY"),
                        true,
                        collegeA.getId(),
                        progA1.getId()
                );

        mockMvc.perform(put("/api/v1/users/" + faculty.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("faculty.john.updated@test.com"))
                .andExpect(jsonPath("$.collegeId").value(collegeA.getId()))
                .andExpect(jsonPath("$.programId").value(progA1.getId()));

        FacultyProfile fp = facultyProfileRepository.findByUserId(faculty.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(fp.getCollege().getId()).isEqualTo(collegeA.getId());
        org.assertj.core.api.Assertions.assertThat(fp.getProgram().getId()).isEqualTo(progA1.getId());
    }
}
