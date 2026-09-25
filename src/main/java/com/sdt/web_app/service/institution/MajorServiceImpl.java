package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.MajorDtos.*;
import com.sdt.web_app.entities.institution.Major;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.MajorRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepository;
    private final ProgramRepository programRepository;

    @Override
    public MajorDetailResponse createMajor(CreateMajorRequest request) {
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + request.programId()));

        String code = request.code().trim().toUpperCase();
        if (majorRepository.existsByProgramIdAndCode(request.programId(), code)) {
            throw new IllegalArgumentException(String.format("Major with code '%s' already exists for program ID %d", code, request.programId()));
        }

        Major major = Major.builder()
                .program(program)
                .code(code)
                .name(request.name().trim())
                .description(request.description())
                .isActive(true)
                .build();

        Major saved = majorRepository.save(major);
        return mapToDetailResponse(saved);
    }

    @Override
    public MajorDetailResponse updateMajor(Long id, UpdateMajorRequest request) {
        Major major = findMajorById(id);
        major.updateDetails(request.name(), request.description(), request.isActive());
        return mapToDetailResponse(major);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MajorSummaryResponse> getMajorsByProgram(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new EntityNotFoundException("Program not found with ID: " + programId);
        }
        return majorRepository.findByProgramId(programId).stream()
                .map(this::mapToSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MajorSummaryResponse> getActiveMajorsByProgram(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new EntityNotFoundException("Program not found with ID: " + programId);
        }
        return majorRepository.findByProgramIdAndIsActiveTrue(programId).stream()
                .map(this::mapToSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MajorDetailResponse getMajorById(Long id) {
        return mapToDetailResponse(findMajorById(id));
    }

    @Override
    public void deleteMajor(Long id) {
        Major major = findMajorById(id);
        majorRepository.delete(major);
    }

    private Major findMajorById(Long id) {
        return majorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Major not found with ID: " + id));
    }

    private MajorSummaryResponse mapToSummaryResponse(Major m) {
        return new MajorSummaryResponse(
                m.getId(),
                m.getProgram().getId(),
                m.getProgram().getCode(),
                m.getCode(),
                m.getName(),
                m.getIsActive()
        );
    }

    private MajorDetailResponse mapToDetailResponse(Major m) {
        return new MajorDetailResponse(
                m.getId(),
                m.getProgram().getId(),
                m.getProgram().getCode(),
                m.getCode(),
                m.getName(),
                m.getDescription(),
                m.getIsActive(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
