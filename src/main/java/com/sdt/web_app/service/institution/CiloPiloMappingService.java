package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CiloPiloMappingDtos.*;
import com.sdt.web_app.entities.institution.CiloPiloMapping;
import com.sdt.web_app.entities.institution.CourseOutcome;
import com.sdt.web_app.entities.institution.ProgramOutcome;
import com.sdt.web_app.repositories.institution.CiloPiloMappingRepository;
import com.sdt.web_app.repositories.institution.CourseOutcomeRepository;
import com.sdt.web_app.repositories.institution.ProgramOutcomeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CiloPiloMappingService {

    private final CiloPiloMappingRepository mappingRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgramOutcomeRepository programOutcomeRepository;

    public CiloPiloMappingResponse createOrUpdateMapping(CreateCiloPiloMappingRequest request) {
        CourseOutcome courseOutcome = courseOutcomeRepository.findById(request.courseOutcomeId())
                .orElseThrow(() -> new IllegalArgumentException("Course outcome not found with ID: " + request.courseOutcomeId()));

        ProgramOutcome programOutcome = programOutcomeRepository.findById(request.programOutcomeId())
                .orElseThrow(() -> new IllegalArgumentException("Program outcome not found with ID: " + request.programOutcomeId()));

        String mappingType = request.mappingType().trim().toUpperCase();
        if (!mappingType.matches("^[IED]$")) {
            throw new IllegalArgumentException("Mapping type must be 'I', 'E', or 'D'");
        }

        Optional<CiloPiloMapping> existing = mappingRepository.findByCourseOutcomeIdAndProgramOutcomeId(
                request.courseOutcomeId(),
                request.programOutcomeId()
        );

        CiloPiloMapping mapping;
        if (existing.isPresent()) {
            mapping = existing.get();
            mapping.updateMappingType(mappingType);
        } else {
            mapping = CiloPiloMapping.builder()
                    .courseOutcome(courseOutcome)
                    .programOutcome(programOutcome)
                    .mappingType(mappingType)
                    .build();
            mapping = mappingRepository.save(mapping);
        }

        return mapToResponse(mapping);
    }

    public void deleteMapping(Long id) {
        CiloPiloMapping mapping = mappingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("CILO-PILO mapping not found with ID: " + id));
        mappingRepository.delete(mapping);
    }

    @Transactional(readOnly = true)
    public List<CiloPiloMappingResponse> getMappingsByCourseOutcomeId(Long courseOutcomeId) {
        if (!courseOutcomeRepository.existsById(courseOutcomeId)) {
            throw new IllegalArgumentException("Course outcome not found with ID: " + courseOutcomeId);
        }
        return mappingRepository.findByCourseOutcomeId(courseOutcomeId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CiloPiloMappingResponse> getMappingsByProgramOutcomeId(Long programOutcomeId) {
        if (!programOutcomeRepository.existsById(programOutcomeId)) {
            throw new IllegalArgumentException("Program outcome not found with ID: " + programOutcomeId);
        }
        return mappingRepository.findByProgramOutcomeId(programOutcomeId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CiloPiloMappingResponse> getMappingsByCourseId(Long courseId) {
        return mappingRepository.findByCourseId(courseId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CiloPiloMappingResponse> getMappingsByProgramId(Long programId) {
        return mappingRepository.findByProgramId(programId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CiloPiloMappingResponse> getMatrixMappings(Long courseId, Long programId) {
        List<CiloPiloMapping> mappings;
        if (courseId != null && programId != null) {
            mappings = mappingRepository.findByCourseIdAndProgramId(courseId, programId);
        } else if (courseId != null) {
            mappings = mappingRepository.findByCourseId(courseId);
        } else if (programId != null) {
            mappings = mappingRepository.findByProgramId(programId);
        } else {
            mappings = mappingRepository.findAll();
        }
        return mappings.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CiloPiloMappingResponse mapToResponse(CiloPiloMapping m) {
        return new CiloPiloMappingResponse(
                m.getId(),
                m.getCourseOutcome().getId(),
                m.getCourseOutcome().getCode(),
                m.getProgramOutcome().getId(),
                m.getProgramOutcome().getCode(),
                m.getMappingType()
        );
    }
}
