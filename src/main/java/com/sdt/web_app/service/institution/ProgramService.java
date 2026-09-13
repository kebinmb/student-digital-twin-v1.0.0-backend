package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.ProgramDtos.*;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.DepartmentType;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.ProgramOutcome;
import com.sdt.web_app.repositories.institution.CiloPiloMappingRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramOutcomeRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.service.security.AcademicScopeAssertionService;
import com.sdt.web_app.service.security.AcademicScopeContext;
import com.sdt.web_app.specifications.ProgramSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AcademicScopeAssertionService academicScopeAssertionService;

    @CacheEvict(value = {"programs", "programsById"}, allEntries = true)
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

        Department college = (department.getType() == DepartmentType.COLLEGE)
                ? department
                : (department.getParentDepartment() != null ? department.getParentDepartment() : department);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateCollegeMutation(scope, college.getId());
        }

        if (programRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Program with code already exists: " + code);
        }

        Program program = Program.builder()
                .department(department)
                .college(college)
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

    @CacheEvict(value = {"programs", "programsById"}, allEntries = true)
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateProgramMutation(scope, id);
        }

        program.updateProgramInfo(name, major, cmoRef, permit, totalUnits);
        return mapToProgramResponse(program);
    }

    @CacheEvict(value = {"programs", "programsById"}, allEntries = true)
    public void deleteProgram(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateProgramMutation(scope, id);
        }

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope.isDean()) {
                return programRepository.findAll(ProgramSpecifications.hasCollegeId(scope.collegeId()).and(ProgramSpecifications.isActive(true)))
                        .stream().map(this::mapToProgramResponse).toList();
            } else if (scope.isChairperson()) {
                return programRepository.findAll(ProgramSpecifications.hasProgramId(scope.programId()).and(ProgramSpecifications.isActive(true)))
                        .stream().map(this::mapToProgramResponse).toList();
            }
        }
        return programRepository.findByIsActiveTrue().stream()
                .map(this::mapToProgramResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "programsById", key = "#id")
    public ProgramResponse getProgramById(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateProgramMutation(scope, id);
        }

        return mapToProgramResponse(program);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "programs", key = "'dept:' + #departmentId")
    public List<ProgramResponse> getProgramsByDepartment(Long departmentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope.isDean()) {
                return programRepository.findAll(ProgramSpecifications.hasDepartmentId(departmentId).and(ProgramSpecifications.hasCollegeId(scope.collegeId())))
                        .stream().map(this::mapToProgramResponse).toList();
            } else if (scope.isChairperson()) {
                return programRepository.findAll(ProgramSpecifications.hasDepartmentId(departmentId).and(ProgramSpecifications.hasProgramId(scope.programId())))
                        .stream().map(this::mapToProgramResponse).toList();
            }
        }
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
        Long collegeId = p.getCollege() != null ? p.getCollege().getId()
                : (p.getDepartment() != null && p.getDepartment().getParentDepartment() != null
                    ? p.getDepartment().getParentDepartment().getId()
                    : (p.getDepartment() != null ? p.getDepartment().getId() : null));

        String collegeCode = p.getCollege() != null ? p.getCollege().getCode()
                : (p.getDepartment() != null && p.getDepartment().getParentDepartment() != null
                    ? p.getDepartment().getParentDepartment().getCode()
                    : (p.getDepartment() != null ? p.getDepartment().getCode() : null));

        return new ProgramResponse(
                p.getId(),
                p.getDepartment() != null ? p.getDepartment().getId() : null,
                p.getDepartment() != null ? p.getDepartment().getCode() : null,
                collegeId,
                collegeCode,
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
