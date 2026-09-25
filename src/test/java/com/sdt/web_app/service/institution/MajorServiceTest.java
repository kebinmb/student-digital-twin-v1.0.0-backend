package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.MajorDtos.*;
import com.sdt.web_app.dto.institution.CurriculumDtos.*;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Major;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.MajorRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import jakarta.persistence.EntityNotFoundException;
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
class MajorServiceTest {

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private MajorServiceImpl majorService;

    private Program program;
    private Major major;

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
                .description("Secondary Mathematics Teaching Specialization")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should create major successfully when inputs are valid")
    void shouldCreateMajorSuccessfully() {
        CreateMajorRequest request = new CreateMajorRequest(1L, "MATH", "Mathematics", "Specialization in Mathematics");

        given(programRepository.findById(1L)).willReturn(Optional.of(program));
        given(majorRepository.existsByProgramIdAndCode(1L, "MATH")).willReturn(false);
        given(majorRepository.save(any(Major.class))).willAnswer(invocation -> {
            Major m = invocation.getArgument(0);
            return Major.builder()
                    .id(10L)
                    .program(m.getProgram())
                    .code(m.getCode())
                    .name(m.getName())
                    .description(m.getDescription())
                    .isActive(m.getIsActive())
                    .build();
        });

        MajorDetailResponse response = majorService.createMajor(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.code()).isEqualTo("MATH");
        assertThat(response.name()).isEqualTo("Mathematics");
        assertThat(response.programCode()).isEqualTo("BSEd");
        verify(majorRepository).save(any(Major.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when creating major for non-existent program")
    void shouldThrowWhenProgramNotFound() {
        CreateMajorRequest request = new CreateMajorRequest(999L, "MATH", "Mathematics", null);
        given(programRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> majorService.createMajor(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Program not found with ID: 999");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when major code already exists in program")
    void shouldThrowWhenMajorCodeAlreadyExists() {
        CreateMajorRequest request = new CreateMajorRequest(1L, "MATH", "Mathematics", null);
        given(programRepository.findById(1L)).willReturn(Optional.of(program));
        given(majorRepository.existsByProgramIdAndCode(1L, "MATH")).willReturn(true);

        assertThatThrownBy(() -> majorService.createMajor(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Major with code 'MATH' already exists");
    }

    @Test
    @DisplayName("Should update major details successfully")
    void shouldUpdateMajorSuccessfully() {
        UpdateMajorRequest request = new UpdateMajorRequest("Mathematics Updated", "Updated Description", true);
        given(majorRepository.findById(10L)).willReturn(Optional.of(major));

        MajorDetailResponse response = majorService.updateMajor(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Mathematics Updated");
        assertThat(response.description()).isEqualTo("Updated Description");
    }

    @Test
    @DisplayName("Should retrieve majors by program")
    void shouldGetMajorsByProgram() {
        given(programRepository.existsById(1L)).willReturn(true);
        given(majorRepository.findByProgramId(1L)).willReturn(List.of(major));

        List<MajorSummaryResponse> responses = majorService.getMajorsByProgram(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).code()).isEqualTo("MATH");
    }

    @Test
    @DisplayName("Should delete major by ID")
    void shouldDeleteMajor() {
        given(majorRepository.findById(10L)).willReturn(Optional.of(major));

        majorService.deleteMajor(10L);

        verify(majorRepository).delete(major);
    }
}
