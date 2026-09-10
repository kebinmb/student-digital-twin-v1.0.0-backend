package com.sdt.web_app.service.grade;

import com.sdt.web_app.BaseIntegrationTest;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.grade.ClassRecordItem;
import com.sdt.web_app.entities.grade.SectionGradingCategory;
import com.sdt.web_app.entities.grade.SectionGradingConfig;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.DepartmentType;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.grade.ClassRecordItemRepository;
import com.sdt.web_app.repositories.grade.SectionGradingCategoryRepository;
import com.sdt.web_app.repositories.grade.SectionGradingConfigRepository;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ClassRecordMatrixIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassSectionRepository sectionRepository;

    @Autowired
    private SectionGradingConfigRepository configRepository;

    @Autowired
    private SectionGradingCategoryRepository categoryRepository;

    @Autowired
    private ClassRecordItemRepository itemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private EnrollmentCourseItemRepository enrollmentCourseItemRepository;

    private ClassSection testSection;

    @BeforeEach
    void setUp() {
        Campus campus = campusRepository.save(Campus.builder()
                .code("CAMPUS-CR")
                .name("Main Campus")
                .region("Region VI")
                .build());

        Department dept = departmentRepository.save(Department.builder()
                .campus(campus)
                .code("CS-DEPT")
                .name("Computer Science Dept")
                .type(DepartmentType.COLLEGE)
                .build());

        Program program = programRepository.save(Program.builder()
                .department(dept)
                .code("BSIT")
                .name("Bachelor of Science in Information Technology")
                .degreeLevel("UNDERGRADUATE")
                .totalUnitsRequired(140)
                .build());

        Curriculum curriculum = curriculumRepository.save(Curriculum.builder()
                .program(program)
                .code("CURR-BSIT-2026")
                .name("BSIT Curriculum 2026")
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.ACTIVE)
                .versionNumber(1)
                .build());

        Course course = courseRepository.save(Course.builder()
                .code("IT-101")
                .title("Intro to Computing")
                .creditUnits(new BigDecimal("3.00"))
                .build());

        AcademicYear ay = academicYearRepository.save(AcademicYear.builder()
                .code("AY 2026-2027")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .isCurrent(true)
                .build());

        Term term = termRepository.save(Term.builder()
                .academicYear(ay)
                .termType(TermType.FIRST_SEM)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(4))
                .isActive(true)
                .build());

        User facultyUser = userRepository.save(User.builder()
                .username("faculty")
                .password("encoded_pass")
                .email("faculty@test.com")
                .build());

        testSection = sectionRepository.save(ClassSection.builder()
                .sectionCode("BSIT-1A")
                .curriculum(curriculum)
                .course(course)
                .term(term)
                .primaryInstructor(facultyUser)
                .gradeStatus(ClassSection.GradeStatus.DRAFT)
                .build());

        SectionGradingConfig config = configRepository.save(SectionGradingConfig.builder()
                .section(testSection)
                .midtermWeight(new BigDecimal("50.00"))
                .finalWeight(new BigDecimal("50.00"))
                .categories(new ArrayList<>())
                .build());

        SectionGradingCategory cat1 = categoryRepository.save(SectionGradingCategory.builder()
                .config(config)
                .categoryName("Quizzes")
                .weightPercentage(new BigDecimal("40.00"))
                .termPeriod(SectionGradingCategory.TermPeriod.MIDTERM)
                .displayOrder(1)
                .items(new ArrayList<>())
                .build());

        SectionGradingCategory cat2 = categoryRepository.save(SectionGradingCategory.builder()
                .config(config)
                .categoryName("Exams")
                .weightPercentage(new BigDecimal("60.00"))
                .termPeriod(SectionGradingCategory.TermPeriod.MIDTERM)
                .displayOrder(2)
                .items(new ArrayList<>())
                .build());

        config.getCategories().add(cat1);
        config.getCategories().add(cat2);

        ClassRecordItem item1 = itemRepository.save(ClassRecordItem.builder()
                .category(cat1)
                .itemTitle("Quiz 1")
                .maxPoints(new BigDecimal("50.00"))
                .sequenceOrder(1)
                .build());

        ClassRecordItem item2 = itemRepository.save(ClassRecordItem.builder()
                .category(cat1)
                .itemTitle("Quiz 2")
                .maxPoints(new BigDecimal("50.00"))
                .sequenceOrder(2)
                .build());

        ClassRecordItem item3 = itemRepository.save(ClassRecordItem.builder()
                .category(cat2)
                .itemTitle("Midterm Exam")
                .maxPoints(new BigDecimal("100.00"))
                .sequenceOrder(1)
                .build());

        cat1.getItems().add(item1);
        cat1.getItems().add(item2);
        cat2.getItems().add(item3);

        User user = userRepository.save(User.builder()
                .username("student_matrix_user")
                .password("encoded_pass")
                .email("student_matrix@test.com")
                .build());

        StudentProfile student = studentProfileRepository.save(StudentProfile.builder()
                .user(user)
                .studentNumber("2026-IT-0099")
                .program(program)
                .curriculum(curriculum)
                .yearLevel(1)
                .build());

        StudentEnrollment enrollment = studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.ENROLLED)
                .build());

        enrollmentCourseItemRepository.save(EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(testSection)
                .completionStatus(EnrollmentCourseItem.CompletionStatus.ENROLLED)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/class-records/sections/{sectionId}/matrix should return HTTP 200 without throwing MultipleBagFetchException")
    @WithMockUser(username = "faculty", roles = {"FACULTY"})
    void getScoreMatrix_Success_NoMultipleBagFetchException() throws Exception {
        mockMvc.perform(get("/api/v1/class-records/sections/" + testSection.getId() + "/matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sectionId").value(testSection.getId()))
                .andExpect(jsonPath("$.sectionCode").value("BSIT-1A"))
                .andExpect(jsonPath("$.config.categories", hasSize(2)))
                .andExpect(jsonPath("$.config.categories[0].categoryName").value("Quizzes"))
                .andExpect(jsonPath("$.config.categories[0].items", hasSize(2)))
                .andExpect(jsonPath("$.config.categories[1].categoryName").value("Exams"))
                .andExpect(jsonPath("$.config.categories[1].items", hasSize(1)))
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].studentNumber").value("2026-IT-0099"));
    }
}
