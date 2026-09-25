package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CurriculumDtos.*;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Major;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.MajorRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CurriculumMajorLinkingTest {

    @Mock
    private CurriculumRepository curriculumRepository;

    @Mock
    private CurriculumCourseRepository curriculumCourseRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MajorRepository majorRepository;

    @InjectMocks
    private CurriculumService curriculumService;

    private Program program;
    private Major major;
    private Curriculum curriculum;

    @BeforeEach
    void setUp() {
        program = Program.builder()
                .id(1L)
                .code("BSEd")
                .name("Bachelor of Secondary Education")
                .totalUnitsRequired(152)
                .isActive(true)
                .build();

        major = Major.builder()
                .id(10L)
                .program(program)
                .code("MATH")
                .name("Mathematics")
                .isActive(true)
                .build();

        curriculum = Curriculum.builder()
                .id(100L)
                .program(program)
                .major(major)
                .code("CURR-BSED-MATH-2026")
                .name("BSEd Major in Mathematics Curriculum")
                .effectiveAcademicYear("AY-2026-2027")
                .status(Curriculum.Status.DRAFT)
                .versionNumber(1)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should create curriculum with major linkage successfully")
    void shouldCreateCurriculumWithMajor() {
        CreateCurriculumRequest request = new CreateCurriculumRequest(
                1L, 10L, "CURR-BSED-MATH-2026", "BSEd Major in Mathematics Curriculum", "AY-2026-2027"
        );

        given(programRepository.findById(1L)).willReturn(Optional.of(program));
        given(curriculumRepository.existsByCode("CURR-BSED-MATH-2026")).willReturn(false);
        given(majorRepository.findById(10L)).willReturn(Optional.of(major));
        given(curriculumRepository.save(any(Curriculum.class))).willReturn(curriculum);

        CurriculumResponse response = curriculumService.createCurriculum(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.majorId()).isEqualTo(10L);
        assertThat(response.majorName()).isEqualTo("Mathematics");
        assertThat(response.programCode()).isEqualTo("BSEd");
        verify(curriculumRepository).save(any(Curriculum.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when major does not belong to program")
    void shouldThrowWhenMajorDoesNotBelongToProgram() {
        Program otherProgram = Program.builder().id(2L).code("BSIT").name("BSIT").totalUnitsRequired(146).build();
        Major otherMajor = Major.builder().id(20L).program(otherProgram).code("NET").name("Networking").build();

        CreateCurriculumRequest request = new CreateCurriculumRequest(
                1L, 20L, "CURR-BSED-MISMATCH", "Mismatch Curriculum", "AY-2026-2027"
        );

        given(programRepository.findById(1L)).willReturn(Optional.of(program));
        given(curriculumRepository.existsByCode("CURR-BSED-MISMATCH")).willReturn(false);
        given(majorRepository.findById(20L)).willReturn(Optional.of(otherMajor));

        assertThatThrownBy(() -> curriculumService.createCurriculum(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Major does not belong to program ID: 1");
    }

    @Test
    @DisplayName("Should retrieve curricula filtered by program and major")
    void shouldGetCurriculaByProgramAndMajor() {
        given(programRepository.existsById(1L)).willReturn(true);
        given(curriculumRepository.findByProgramIdAndMajorIdAndIsActiveTrueOrderByCodeAsc(1L, 10L))
                .willReturn(List.of(curriculum));

        List<CurriculumResponse> results = curriculumService.getCurriculaByProgramAndMajor(1L, 10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).majorId()).isEqualTo(10L);
        assertThat(results.get(0).code()).isEqualTo("CURR-BSED-MATH-2026");
    }
}
