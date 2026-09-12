package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.ClassRecordDtos.*;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.grade.ClassRecordItem;
import com.sdt.web_app.entities.grade.SectionGradingCategory;
import com.sdt.web_app.entities.grade.SectionGradingConfig;
import com.sdt.web_app.entities.institution.GradingScale;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassRecordInProgressGradingTest {

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

    private ClassSection mockSection;
    private SectionGradingConfig mockConfig;
    private StudentProfile mockStudent;
    private SectionGradingCategory quizCategory;
    private ClassRecordItem quizItem;

    @BeforeEach
    void setUp() {
        mockSection = ClassSection.builder()
                .id(100L)
                .sectionCode("IT-3A")
                .gradeStatus(ClassSection.GradeStatus.DRAFT)
                .build();

        mockConfig = SectionGradingConfig.builder()
                .id(200L)
                .section(mockSection)
                .midtermWeight(new BigDecimal("50.00"))
                .finalWeight(new BigDecimal("50.00"))
                .build();

        quizCategory = SectionGradingCategory.builder()
                .id(300L)
                .config(mockConfig)
                .categoryName("Quizzes")
                .weightPercentage(new BigDecimal("20.00"))
                .termPeriod(SectionGradingCategory.TermPeriod.MIDTERM)
                .displayOrder(1)
                .build();

        SectionGradingCategory examCategory = SectionGradingCategory.builder()
                .id(301L)
                .config(mockConfig)
                .categoryName("Major Exam")
                .weightPercentage(new BigDecimal("80.00"))
                .termPeriod(SectionGradingCategory.TermPeriod.MIDTERM)
                .displayOrder(2)
                .build();

        // Populate categories via List
        mockConfig.getCategories().addAll(List.of(quizCategory, examCategory));

        quizItem = ClassRecordItem.builder()
                .id(400L)
                .category(quizCategory)
                .itemTitle("Quiz 1")
                .maxPoints(new BigDecimal("20.00"))
                .sequenceOrder(1)
                .build();

        com.sdt.web_app.entities.authentication.User mockUser = com.sdt.web_app.entities.authentication.User.builder()
                .id(500L)
                .username("student_john")
                .build();

        mockStudent = StudentProfile.builder()
                .id(600L)
                .studentNumber("2026-0001")
                .user(mockUser)
                .yearLevel(3)
                .build();
    }

    @Test
    @DisplayName("Gap Item 10: Early partial score (Quiz 1: 20/20) yields progressive 100% standing and 1.00 transmuted grade instead of 5.00 FAILED")
    void testProgressiveWeightedGradingForPartialTerm() {
        com.sdt.web_app.entities.enrollment.StudentEnrollment mockEnrollment = com.sdt.web_app.entities.enrollment.StudentEnrollment.builder()
                .id(700L)
                .student(mockStudent)
                .build();

        com.sdt.web_app.entities.enrollment.EnrollmentCourseItem mockItem = com.sdt.web_app.entities.enrollment.EnrollmentCourseItem.builder()
                .id(800L)
                .enrollment(mockEnrollment)
                .section(mockSection)
                .completionStatus(com.sdt.web_app.entities.enrollment.EnrollmentCourseItem.CompletionStatus.ENROLLED)
                .build();

        com.sdt.web_app.entities.grade.StudentAssessmentScore score = com.sdt.web_app.entities.grade.StudentAssessmentScore.builder()
                .id(900L)
                .item(quizItem)
                .student(mockStudent)
                .scoreEarned(new BigDecimal("20.00"))
                .isExcused(false)
                .build();

        when(sectionRepository.findByIdWithSchedules(100L)).thenReturn(Optional.of(mockSection));
        when(configRepository.findBySectionIdWithDetails(100L)).thenReturn(Optional.of(mockConfig));
        when(enrollmentItemRepository.findBySectionIdWithStudentDetails(100L)).thenReturn(List.of(mockItem));
        when(itemRepository.findBySectionId(100L)).thenReturn(List.of(quizItem));
        when(scoreRepository.findBySectionId(100L)).thenReturn(List.of(score));

        GradingScale passingScale = GradingScale.builder()
                .id(1L)
                .code("1.00")
                .numericGrade(new BigDecimal("1.00"))
                .percentageMin(new BigDecimal("97.00"))
                .percentageMax(new BigDecimal("100.00"))
                .transmutedGrade("1.00")
                .remarks("EXCELLENT")
                .isPassing(true)
                .build();

        when(transmutationService.transmutePercentage(any(BigDecimal.class))).thenReturn(passingScale);

        ClassRecordMatrixResponse matrix = classRecordService.getScoreMatrix(100L);

        assertThat(matrix.rows()).hasSize(1);
        StudentScoreMatrixRowDto row = matrix.rows().get(0);

        // Verify Midterm & Total Raw Percentage are progressively evaluated to 100.00%
        assertThat(row.midtermRawPercentage()).isEqualByComparingTo("100.00");
        assertThat(row.totalRawPercentage()).isEqualByComparingTo("100.00");
        assertThat(row.transmutedGrade()).isEqualByComparingTo("1.00");
        assertThat(row.completionStatus()).isEqualTo("IN_PROGRESS");
    }
}
