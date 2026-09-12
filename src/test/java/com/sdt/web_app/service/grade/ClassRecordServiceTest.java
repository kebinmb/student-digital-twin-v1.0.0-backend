package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.ClassRecordDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.grade.ClassRecordItem;
import com.sdt.web_app.entities.grade.SectionGradingCategory;
import com.sdt.web_app.entities.grade.SectionGradingConfig;
import com.sdt.web_app.entities.grade.StudentAssessmentScore;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.GradingScale;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.grade.ClassRecordItemRepository;
import com.sdt.web_app.repositories.grade.SectionGradingCategoryRepository;
import com.sdt.web_app.repositories.grade.SectionGradingConfigRepository;
import com.sdt.web_app.repositories.grade.StudentAssessmentScoreRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.service.institution.GradeTransmutationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClassRecordServiceTest {

    @Mock
    private ClassSectionRepository sectionRepository;
    @Mock
    private SectionGradingConfigRepository configRepository;
    @Mock
    private SectionGradingCategoryRepository categoryRepository;
    @Mock
    private ClassRecordItemRepository itemRepository;
    @Mock
    private StudentAssessmentScoreRepository scoreRepository;
    @Mock
    private EnrollmentCourseItemRepository enrollmentItemRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private GradeTransmutationService transmutationService;

    @InjectMocks
    private ClassRecordService classRecordService;

    private ClassSection section;
    private SectionGradingConfig config;
    private SectionGradingCategory category;
    private ClassRecordItem item;
    private StudentProfile studentProfile;
    private EnrollmentCourseItem enrollmentItem;

    @BeforeEach
    void setUp() {
        Course course = Course.builder()
                .code("CS-311")
                .title("Software Engineering")
                .creditUnits(new BigDecimal("3.00"))
                .build();
        ReflectionTestUtils.setField(course, "id", 101L);

        Term term = Term.builder().build();
        ReflectionTestUtils.setField(term, "id", 10L);

        section = ClassSection.builder()
                .sectionCode("BSIT-3A")
                .course(course)
                .term(term)
                .gradeStatus(ClassSection.GradeStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(section, "id", 201L);

        config = SectionGradingConfig.builder()
                .section(section)
                .midtermWeight(new BigDecimal("50.00"))
                .finalWeight(new BigDecimal("50.00"))
                .build();
        ReflectionTestUtils.setField(config, "id", 301L);

        category = SectionGradingCategory.builder()
                .config(config)
                .categoryName("Quizzes")
                .weightPercentage(new BigDecimal("100.00"))
                .termPeriod(SectionGradingCategory.TermPeriod.MIDTERM)
                .build();
        ReflectionTestUtils.setField(category, "id", 401L);
        config.getCategories().add(category);

        item = ClassRecordItem.builder()
                .category(category)
                .itemTitle("Quiz 1")
                .maxPoints(new BigDecimal("50.00"))
                .build();
        ReflectionTestUtils.setField(item, "id", 501L);
        category.getItems().add(item);

        User studentUser = User.builder().username("juan_delacruz").build();
        studentProfile = StudentProfile.builder()
                .user(studentUser)
                .studentNumber("2026-CS-0001")
                .program(Program.builder().code("BSIT").build())
                .build();
        ReflectionTestUtils.setField(studentProfile, "id", 601L);

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(studentProfile)
                .term(term)
                .build();

        enrollmentItem = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(section)
                .completionStatus(EnrollmentCourseItem.CompletionStatus.ENROLLED)
                .build();
        ReflectionTestUtils.setField(enrollmentItem, "id", 701L);
    }

    @Test
    @DisplayName("Should retrieve or initialize default section grading config")
    void getGradingConfig_Success() {
        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));
        given(configRepository.findBySectionIdWithDetails(201L)).willReturn(Optional.of(config));

        SectionGradingConfigResponse response = classRecordService.getGradingConfig(201L);

        assertThat(response).isNotNull();
        assertThat(response.sectionId()).isEqualTo(201L);
        assertThat(response.midtermWeight()).isEqualByComparingTo("50.00");
        assertThat(response.finalWeight()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Should add new assessment item to section category")
    void addAssessmentItem_Success() {
        CreateClassRecordItemRequest request = new CreateClassRecordItemRequest(401L, "Quiz 2", new BigDecimal("100.00"), 2);

        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));
        given(categoryRepository.findById(401L)).willReturn(Optional.of(category));
        given(itemRepository.save(any(ClassRecordItem.class))).willAnswer(inv -> inv.getArgument(0));

        ClassRecordItemDto result = classRecordService.addAssessmentItem(201L, request, 5L);

        assertThat(result).isNotNull();
        assertThat(result.itemTitle()).isEqualTo("Quiz 2");
        assertThat(result.maxPoints()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Should calculate raw scores and transmute to CHED numerical grade")
    void getScoreMatrix_Success() {
        given(sectionRepository.findByIdWithSchedules(201L)).willReturn(Optional.of(section));
        given(configRepository.findBySectionIdWithDetails(201L)).willReturn(Optional.of(config));
        given(enrollmentItemRepository.findBySectionIdWithStudentDetails(201L)).willReturn(List.of(enrollmentItem));
        given(itemRepository.findBySectionId(201L)).willReturn(List.of(item));

        StudentAssessmentScore score = StudentAssessmentScore.builder()
                .item(item)
                .student(studentProfile)
                .scoreEarned(new BigDecimal("45.00")) // 45 / 50 = 90%
                .build();
        given(scoreRepository.findBySectionId(201L)).willReturn(List.of(score));

        GradingScale scale = GradingScale.builder()
                .code("1.75")
                .numericGrade(new BigDecimal("1.75"))
                .isPassing(true)
                .build();
        given(transmutationService.transmutePercentage(any(BigDecimal.class))).willReturn(scale);

        ClassRecordMatrixResponse matrix = classRecordService.getScoreMatrix(201L);

        assertThat(matrix).isNotNull();
        assertThat(matrix.rows()).hasSize(1);
        StudentScoreMatrixRowDto row = matrix.rows().get(0);
        assertThat(row.studentId()).isEqualTo(601L);
        assertThat(row.transmutedGrade()).isEqualByComparingTo("1.75");
        assertThat(row.completionStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Should reject config update when section is SUBMITTED")
    void updateGradingConfig_WhenSubmitted_ThrowsException() {
        section.updateGradeStatus(ClassSection.GradeStatus.SUBMITTED);
        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));

        UpdateSectionGradingConfigRequest request = new UpdateSectionGradingConfigRequest(
                new BigDecimal("50.00"), new BigDecimal("50.00"), List.of());

        assertThatThrownBy(() -> classRecordService.updateGradingConfig(201L, request, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Class record modifications blocked");
    }

    @Test
    @DisplayName("BUG-01: Should batch save raw scores using bulk loading and saveAll")
    void batchSaveScores_BulkSuccess() {
        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));
        given(itemRepository.findAllById(any())).willReturn(List.of(item));
        given(studentProfileRepository.findAllById(any())).willReturn(List.of(studentProfile));
        given(scoreRepository.findBySectionId(201L)).willReturn(List.of());

        // For subsequent recalculateAndSyncSectionGrades and getScoreMatrix:
        given(sectionRepository.findByIdWithSchedules(201L)).willReturn(Optional.of(section));
        given(configRepository.findBySectionIdWithDetails(201L)).willReturn(Optional.of(config));
        given(enrollmentItemRepository.findBySectionIdWithStudentDetails(201L)).willReturn(List.of(enrollmentItem));
        given(itemRepository.findBySectionId(201L)).willReturn(List.of(item));

        BatchSaveScoresRequest request = new BatchSaveScoresRequest(List.of(
                new StudentScoreEntryDto(501L, 601L, new BigDecimal("48.00"), false)
        ));

        ClassRecordMatrixResponse response = classRecordService.batchSaveScores(201L, request, 5L);

        assertThat(response).isNotNull();
        verify(scoreRepository).saveAll(any());
    }

    @Test
    @DisplayName("BUG-03: Should delete assessment item and immediately trigger recalculateAndSyncSectionGrades")
    void deleteAssessmentItem_TriggersRecalculate() {
        given(itemRepository.findById(501L)).willReturn(Optional.of(item));
        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));
        given(sectionRepository.findByIdWithSchedules(201L)).willReturn(Optional.of(section));
        given(configRepository.findBySectionIdWithDetails(201L)).willReturn(Optional.of(config));
        given(enrollmentItemRepository.findBySectionIdWithStudentDetails(201L)).willReturn(List.of(enrollmentItem));
        given(itemRepository.findBySectionId(201L)).willReturn(List.of());
        given(scoreRepository.findBySectionId(201L)).willReturn(List.of());

        classRecordService.deleteAssessmentItem(501L, 5L);

        verify(scoreRepository).deleteByItemId(501L);
        verify(itemRepository).delete(item);
        verify(enrollmentItemRepository, atLeastOnce()).findBySectionIdWithStudentDetails(201L);
    }
}
