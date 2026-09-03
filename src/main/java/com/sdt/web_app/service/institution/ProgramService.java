package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.ProgramDtos.*;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.ProgramOutcome;
import com.sdt.web_app.repositories.institution.CiloPiloMappingRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramOutcomeRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramOutcomeRepository programOutcomeRepository;
    private final DepartmentRepository departmentRepository;
    private final CurriculumRepository curriculumRepository;
    private final CiloPiloMappingRepository ciloPiloMappingRepository;

    public ProgramResponse createProgram(
            Long departmentId,
            String code,
            String name,
            String major,
            String degreeLevel,
            int totalUnitsRequired
    ) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + departmentId));

        if (programRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Program with code already exists: " + code);
        }

        Program program = Program.builder()
                .department(department)
                .code(code.trim().toUpperCase())
                .name(name.trim())
                .major(major)
                .degreeLevel(degreeLevel != null ? degreeLevel : "UNDERGRADUATE")
                .totalUnitsRequired(totalUnitsRequired)
                .isActive(true)
                .build();

        Program saved = programRepository.save(program);
        return mapToProgramResponse(saved);
    }

    public ProgramResponse updateProgram(
            Long id,
            String name,
            String major,
            String cmoRef,
            String permit,
            int totalUnits
    ) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + id));

        program.updateProgramInfo(name, major, cmoRef, permit, totalUnits);
        return mapToProgramResponse(program);
    }

    public void deleteProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + id));

        if (curriculumRepository.existsByProgramId(id)) {
            throw new IllegalStateException("Cannot delete program referenced by existing curricula");
        }

        programRepository.delete(program);
    }

    public ProgramOutcomeResponse createProgramOutcome(Long programId, String code, String description) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("Program not found with ID: " + programId));

        if (programOutcomeRepository.existsByProgramIdAndCode(programId, code)) {
            throw new IllegalArgumentException("Program outcome with code " + code + " already exists for program " + program.getCode());
        }

        ProgramOutcome outcome = ProgramOutcome.builder()
                .program(program)
                .code(code.trim().toUpperCase())
                .description(description.trim())
                .build();

        ProgramOutcome saved = programOutcomeRepository.save(outcome);
        return mapToOutcomeResponse(saved);
    }

    public void deleteProgramOutcome(Long id) {
        ProgramOutcome outcome = programOutcomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Program outcome not found with ID: " + id));

        if (ciloPiloMappingRepository.existsByProgramOutcomeId(id)) {
            throw new IllegalStateException("Cannot delete program outcome mapped in CILO-PILO matrix");
        }

        programOutcomeRepository.delete(outcome);
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> getAllPrograms() {
        return programRepository.findByIsActiveTrue().stream()
                .map(this::mapToProgramResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(Long id) {
        return programRepository.findById(id)
                .map(this::mapToProgramResponse)
                .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> getProgramsByDepartment(Long departmentId) {
        return programRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToProgramResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgramOutcomeResponse> getProgramOutcomes(Long programId) {
        if (!programRepository.existsById(programId)) {
            throw new IllegalArgumentException("Program not found with ID: " + programId);
        }
        return programOutcomeRepository.findByProgramId(programId).stream()
                .map(this::mapToOutcomeResponse)
                .toList();
    }

    private ProgramResponse mapToProgramResponse(Program p) {
        return new ProgramResponse(
                p.getId(),
                p.getDepartment().getId(),
                p.getDepartment().getCode(),
                p.getCode(),
                p.getName(),
                p.getMajor(),
                p.getDegreeLevel(),
                p.getTotalUnitsRequired(),
                p.isActive()
        );
    }

    private ProgramOutcomeResponse mapToOutcomeResponse(ProgramOutcome po) {
        return new ProgramOutcomeResponse(
                po.getId(),
                po.getProgram().getId(),
                po.getProgram().getCode(),
                po.getCode(),
                po.getDescription()
        );
    }
}
