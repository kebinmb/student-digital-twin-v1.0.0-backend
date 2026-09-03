package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.DepartmentDtos.*;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final ProgramRepository programRepository;

    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        // Prerequisite check: Campus must exist
        Campus campus = campusRepository.findById(request.campusId())
                .orElseThrow(() -> new IllegalArgumentException("Campus not found with ID: " + request.campusId()));

        if (departmentRepository.existsByCampusIdAndCode(request.campusId(), request.code())) {
            throw new IllegalArgumentException("Department with code " + request.code() + " already exists in campus " + campus.getCode());
        }

        Department parentDepartment = null;
        if (request.parentDepartmentId() != null) {
            parentDepartment = departmentRepository.findById(request.parentDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent department not found with ID: " + request.parentDepartmentId()));

            if (!parentDepartment.getCampus().getId().equals(campus.getId())) {
                throw new IllegalArgumentException("Parent department must belong to the same campus (" + campus.getCode() + ")");
            }
        }

        Department department = Department.builder()
                .campus(campus)
                .code(request.code().trim().toUpperCase())
                .name(request.name().trim())
                .type(request.type())
                .parentDepartment(parentDepartment)
                .deanUserId(request.deanUserId())
                .isActive(true)
                .build();

        Department saved = departmentRepository.save(department);
        return mapToResponse(saved);
    }

    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + id));

        department.updateInfo(request.name(), request.type());
        department.assignDean(request.deanUserId());

        if (request.parentDepartmentId() != null) {
            if (request.parentDepartmentId().equals(id)) {
                throw new IllegalArgumentException("A department cannot be its own parent");
            }
            Department parent = departmentRepository.findById(request.parentDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent department not found with ID: " + request.parentDepartmentId()));

            if (!parent.getCampus().getId().equals(department.getCampus().getId())) {
                throw new IllegalArgumentException("Parent department must belong to the same campus");
            }
            department.assignParentDepartment(parent);
        } else {
            department.assignParentDepartment(null);
        }

        return mapToResponse(department);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + id));

        if (departmentRepository.existsByParentDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department that has child sub-departments");
        }

        if (programRepository.existsByDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department that contains academic degree programs");
        }

        departmentRepository.delete(department);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + id));
        return mapToResponse(dept);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByCampus(Long campusId) {
        if (!campusRepository.existsById(campusId)) {
            throw new IllegalArgumentException("Campus not found with ID: " + campusId);
        }
        return departmentRepository.findByCampusId(campusId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private DepartmentResponse mapToResponse(Department d) {
        Long parentId = d.getParentDepartment() != null ? d.getParentDepartment().getId() : null;
        String parentCode = d.getParentDepartment() != null ? d.getParentDepartment().getCode() : null;

        return new DepartmentResponse(
                d.getId(),
                d.getCampus().getId(),
                d.getCampus().getCode(),
                d.getCampus().getName(),
                d.getCode(),
                d.getName(),
                d.getType(),
                parentId,
                parentCode,
                d.getDeanUserId(),
                d.isActive()
        );
    }
}
