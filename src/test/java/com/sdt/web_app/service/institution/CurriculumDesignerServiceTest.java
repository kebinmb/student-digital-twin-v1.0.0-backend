package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.*;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.institution.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CurriculumDesignerServiceTest {

    @Mock
    private CurriculumRepository curriculumRepository;

    @Mock
    private CurriculumCourseRepository curriculumCourseRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePrerequisiteRepository prerequisiteRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private CurriculumValidationService validationService;

    @InjectMocks
    private CurriculumDesignerService designerService;

    private Program program;
    private Curriculum draftCurriculum;
    private Curriculum approvedCurriculum;
    private Course course;
    private CurriculumCourse curriculumCourse;

    @BeforeEach
    void setUp() {
        program = Program.builder()
                .id(1L)
                .code("BSIT")
                .name("Bachelor of Science in Information Technology")
                .totalUnitsRequired(146)
                .build();

        draftCurriculum = Curriculum.builder()
                .id(10L)
                .program(program)
                .code("BSIT-2026")
                .name("BSIT Curriculum 2026")
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.DRAFT)
                .versionNumber(1)
                .isActive(true)
                .build();

        approvedCurriculum = Curriculum.builder()
                .id(11L)
                .program(program)
                .code("BSIT-2025")
                .name("BSIT Curriculum 2025")
                .effectiveAcademicYear("2025-2026")
                .status(Curriculum.Status.APPROVED)
                .versionNumber(1)
                .isActive(true)
                .build();

        course = Course.builder()
                .id(100L)
                .code("IT 101")
                .title("Introduction to Computing")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .contactHoursLec(2)
                .contactHoursLab(3)
                .isActive(true)
                .build();

        curriculumCourse = CurriculumCourse.builder()
                .id(500L)
                .curriculum(draftCurriculum)
                .course(course)
                .yearLevel(1)
                .semester("1ST_SEM")
                .category("PROFESSIONAL_MAJOR")
                .sequenceOrder(1)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new curriculum in DRAFT status")
    void createCurriculum_Success() {
        CreateCurriculumRequest request = new CreateCurriculumRequest(
                1L, "BSIT-2026", "BSIT Curriculum 2026", "2026-2027"
        );

        given(curriculumRepository.existsByCode("BSIT-2026")).willReturn(false);
        given(programRepository.findById(1L)).willReturn(Optional.of(program));
        given(curriculumRepository.save(any(Curriculum.class))).willAnswer(invocation -> {
            Curriculum arg = invocation.getArgument(0);
            return Curriculum.builder()
                    .id(10L)
                    .program(arg.getProgram())
                    .code(arg.getCode())
                    .name(arg.getName())
                    .effectiveAcademicYear(arg.getEffectiveAcademicYear())
                    .status(arg.getStatus())
                    .versionNumber(arg.getVersionNumber())
                    .isActive(arg.isActive())
                    .build();
        });

        CurriculumSummaryResponse response = designerService.createCurriculum(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.code()).isEqualTo("BSIT-2026");
        assertThat(response.programCode()).isEqualTo("BSIT");
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.versionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return distinct designer view mapped specifically to requested curriculum ID")
    void getDesignerView_ReturnsDistinctCurriculumCourses() {
        Course course2 = Course.builder()
                .id(101L)
                .code("CC-102")
                .title("Fundamentals of Programming")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .contactHoursLec(2)
                .contactHoursLab(3)
                .isActive(true)
                .build();

        CurriculumCourse cc2 = CurriculumCourse.builder()
                .id(501L)
                .curriculum(draftCurriculum)
                .course(course2)
                .yearLevel(1)
                .semester("2ND_SEM")
                .category("PROFESSIONAL_MAJOR")
                .sequenceOrder(1)
                .build();

        given(curriculumRepository.findById(10L)).willReturn(Optional.of(draftCurriculum));
        given(curriculumCourseRepository.findByCurriculumId(10L)).willReturn(List.of(curriculumCourse, cc2));
        given(prerequisiteRepository.findPrerequisitesForCourseIds(List.of(100L, 101L))).willReturn(Collections.emptyList());

        DesignerViewResponse view = designerService.getDesignerView(10L);

        assertThat(view.curriculumId()).isEqualTo(10L);
        assertThat(view.code()).isEqualTo("BSIT-2026");
        assertThat(view.totalUnits()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(view.totalContactHours()).isEqualTo(10);
        assertThat(view.yearBlocks()).hasSize(4);

        YearBlockDto y1 = view.yearBlocks().stream().filter(y -> y.yearLevel() == 1).findFirst().orElseThrow();
        SemesterBlockDto sem1 = y1.semesters().stream().filter(s -> "1ST_SEM".equals(s.semester())).findFirst().orElseThrow();
        assertThat(sem1.courses()).hasSize(1);
        assertThat(sem1.courses().get(0).code()).isEqualTo("IT 101");

        SemesterBlockDto sem2 = y1.semesters().stream().filter(s -> "2ND_SEM".equals(s.semester())).findFirst().orElseThrow();
        assertThat(sem2.courses()).hasSize(1);
        assertThat(sem2.courses().get(0).code()).isEqualTo("CC-102");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when curriculum not found for designer view")
    void getDesignerView_NotFound_ThrowsException() {
        given(curriculumRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> designerService.getDesignerView(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Curriculum not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw exception when creating curriculum with duplicate code")
    void createCurriculum_DuplicateCode_ThrowsException() {
        CreateCurriculumRequest request = new CreateCurriculumRequest(
                1L, "BSIT-2026", "BSIT Curriculum 2026", "2026-2027"
        );

        given(curriculumRepository.existsByCode("BSIT-2026")).willReturn(true);

        assertThatThrownBy(() -> designerService.createCurriculum(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Curriculum code already exists");
    }

    @Test
    @DisplayName("Should successfully add a course to draft curriculum with auto sequence order")
    void addCourseToCurriculum_Success() {
        AddCourseToCurriculumRequest request = new AddCourseToCurriculumRequest(
                100L, 1, "1ST_SEM", "PROFESSIONAL_MAJOR", null
        );

        given(curriculumRepository.findById(10L)).willReturn(Optional.of(draftCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(10L, 100L)).willReturn(false);
        given(curriculumCourseRepository.findByCurriculumIdAndYearLevelAndSemester(10L, 1, "1ST_SEM"))
                .willReturn(Collections.emptyList());

        designerService.addCourseToCurriculum(10L, request);

        verify(curriculumCourseRepository).save(any(CurriculumCourse.class));
    }

    @Test
    @DisplayName("Should throw exception when adding course already in curriculum")
    void addCourseToCurriculum_AlreadyAssigned_ThrowsException() {
        AddCourseToCurriculumRequest request = new AddCourseToCurriculumRequest(
                100L, 1, "1ST_SEM", "PROFESSIONAL_MAJOR", 1
        );

        given(curriculumRepository.findById(10L)).willReturn(Optional.of(draftCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(10L, 100L)).willReturn(true);

        assertThatThrownBy(() -> designerService.addCourseToCurriculum(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Course is already assigned to this curriculum");
    }

    @Test
    @DisplayName("Immutability Guard: Should throw exception when modifying an APPROVED curriculum")
    void addCourseToCurriculum_ApprovedStatus_ThrowsException() {
        AddCourseToCurriculumRequest request = new AddCourseToCurriculumRequest(
                100L, 1, "1ST_SEM", "PROFESSIONAL_MAJOR", 1
        );

        given(curriculumRepository.findById(11L)).willReturn(Optional.of(approvedCurriculum));

        assertThatThrownBy(() -> designerService.addCourseToCurriculum(11L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Curriculum is locked under status");
    }

    @Test
    @DisplayName("Should successfully remove a course from draft curriculum")
    void removeCourseFromCurriculum_Success() {
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(draftCurriculum));
        given(curriculumCourseRepository.findById(500L)).willReturn(Optional.of(curriculumCourse));

        designerService.removeCourseFromCurriculum(10L, 500L);

        verify(curriculumCourseRepository).delete(curriculumCourse);
    }

    @Test
    @DisplayName("Immutability Guard: Should throw exception when removing a course from APPROVED curriculum")
    void removeCourseFromCurriculum_ApprovedStatus_ThrowsException() {
        given(curriculumRepository.findById(11L)).willReturn(Optional.of(approvedCurriculum));

        assertThatThrownBy(() -> designerService.removeCourseFromCurriculum(11L, 500L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Curriculum is locked under status");
    }

    @Test
    @DisplayName("Should clone curriculum into a new revision draft with incremented version number and deep-copied courses")
    void cloneCurriculumAsNewRevision_Success() {
        CloneCurriculumRequest request = new CloneCurriculumRequest(
                "BSIT-2027", "BSIT Curriculum 2027 Revision", "2027-2028"
        );

        given(curriculumRepository.findById(10L)).willReturn(Optional.of(draftCurriculum));
        given(curriculumRepository.existsByCode("BSIT-2027")).willReturn(false);
        given(curriculumRepository.save(any(Curriculum.class))).willAnswer(invocation -> {
            Curriculum arg = invocation.getArgument(0);
            return Curriculum.builder()
                    .id(20L)
                    .program(arg.getProgram())
                    .code(arg.getCode())
                    .name(arg.getName())
                    .effectiveAcademicYear(arg.getEffectiveAcademicYear())
                    .status(arg.getStatus())
                    .versionNumber(arg.getVersionNumber())
                    .isActive(arg.isActive())
                    .build();
        });
        given(curriculumCourseRepository.findByCurriculumId(10L)).willReturn(List.of(curriculumCourse));

        CurriculumSummaryResponse response = designerService.cloneCurriculumAsNewRevision(10L, request);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.code()).isEqualTo("BSIT-2027");
        assertThat(response.versionNumber()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("DRAFT");
        verify(curriculumCourseRepository).save(any(CurriculumCourse.class));
    }

    @Test
    @DisplayName("Should return all active curricula as lookup options ordered by code")
    void getCurriculumLookupOptions_Success() {
        given(curriculumRepository.findByIsActiveTrueOrderByCodeAsc())
                .willReturn(List.of(draftCurriculum, approvedCurriculum));

        List<CurriculumLookupOption> options = designerService.getCurriculumLookupOptions();

        assertThat(options).hasSize(2);
        assertThat(options.get(0).id()).isEqualTo(10L);
        assertThat(options.get(0).code()).isEqualTo("BSIT-2026");
        assertThat(options.get(0).name()).isEqualTo("BSIT Curriculum 2026");
        assertThat(options.get(0).programCode()).isEqualTo("BSIT");
        assertThat(options.get(0).effectiveAcademicYear()).isEqualTo("2026-2027");
        assertThat(options.get(0).status()).isEqualTo("DRAFT");
        assertThat(options.get(0).versionNumber()).isEqualTo(1);

        assertThat(options.get(1).id()).isEqualTo(11L);
        assertThat(options.get(1).code()).isEqualTo("BSIT-2025");
        assertThat(options.get(1).status()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Should successfully delete a DRAFT curriculum and cascade-delete its courses")
    void deleteCurriculum_Draft_Success() {
        given(curriculumRepository.findById(10L)).willReturn(Optional.of(draftCurriculum));
        given(curriculumCourseRepository.findByCurriculumId(10L)).willReturn(List.of(curriculumCourse));

        designerService.deleteCurriculum(10L);

        verify(curriculumCourseRepository).deleteAll(List.of(curriculumCourse));
        verify(curriculumRepository).delete(draftCurriculum);
    }

    @Test
    @DisplayName("Should successfully delete an UNDER_REVIEW curriculum")
    void deleteCurriculum_UnderReview_Success() {
        Curriculum underReviewCurriculum = Curriculum.builder()
                .id(12L)
                .program(program)
                .code("BSIT-2024")
                .name("BSIT Curriculum 2024")
                .effectiveAcademicYear("2024-2025")
                .status(Curriculum.Status.UNDER_REVIEW)
                .versionNumber(1)
                .isActive(true)
                .build();

        given(curriculumRepository.findById(12L)).willReturn(Optional.of(underReviewCurriculum));
        given(curriculumCourseRepository.findByCurriculumId(12L)).willReturn(Collections.emptyList());

        designerService.deleteCurriculum(12L);

        verify(curriculumRepository).delete(underReviewCurriculum);
    }

    @Test
    @DisplayName("Immutability Guard: Should throw IllegalStateException when attempting to delete an ACTIVE curriculum")
    void deleteCurriculum_ActiveStatus_ThrowsIllegalStateException() {
        Curriculum activeCurriculum = Curriculum.builder()
                .id(13L)
                .program(program)
                .code("BSIT-ACTIVE")
                .name("BSIT Active Curriculum")
                .effectiveAcademicYear("2023-2024")
                .status(Curriculum.Status.ACTIVE)
                .versionNumber(1)
                .isActive(true)
                .build();

        given(curriculumRepository.findById(13L)).willReturn(Optional.of(activeCurriculum));

        assertThatThrownBy(() -> designerService.deleteCurriculum(13L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete a curriculum that is ACTIVE or APPROVED");
    }

    @Test
    @DisplayName("Immutability Guard: Should throw IllegalStateException when attempting to delete an APPROVED curriculum")
    void deleteCurriculum_ApprovedStatus_ThrowsIllegalStateException() {
        given(curriculumRepository.findById(11L)).willReturn(Optional.of(approvedCurriculum));

        assertThatThrownBy(() -> designerService.deleteCurriculum(11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete a curriculum that is ACTIVE or APPROVED");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent curriculum")
    void deleteCurriculum_NotFound_ThrowsEntityNotFoundException() {
        given(curriculumRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> designerService.deleteCurriculum(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Curriculum not found with ID: 999");
    }
}
