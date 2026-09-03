package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CampusDtos.*;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CampusService {

    private final CampusRepository campusRepository;
    private final DepartmentRepository departmentRepository;

    public CampusResponse createCampus(CreateCampusRequest request) {
        if (campusRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Campus with code already exists: " + request.code());
        }

        if (request.isMain()) {
            campusRepository.findByIsMainTrue().ifPresent(Campus::demoteFromMainCampus);
        }

        Campus campus = Campus.builder()
                .code(request.code().trim().toUpperCase())
                .name(request.name().trim())
                .chedInstitutionalCode(request.chedInstitutionalCode())
                .address(request.address())
                .region(request.region() != null && !request.region().isBlank() ? request.region() : "REGION VI")
                .contactNumber(request.contactNumber())
                .email(request.email())
                .isMain(request.isMain())
                .isActive(true)
                .build();

        Campus saved = campusRepository.save(campus);
        return mapToResponse(saved);
    }

    public CampusResponse updateCampus(Long id, UpdateCampusRequest request) {
        Campus campus = findEntityById(id);
        campus.updateDetails(
                request.name(),
                request.address(),
                request.contactNumber(),
                request.email(),
                request.chedInstitutionalCode()
        );
        return mapToResponse(campus);
    }

    public CampusResponse toggleCampusActive(Long id, boolean active) {
        Campus campus = findEntityById(id);
        if (active) {
            campus.activate();
        } else {
            campus.deactivate();
        }
        return mapToResponse(campus);
    }

    public void deleteCampus(Long id) {
        Campus campus = findEntityById(id);
        if (departmentRepository.existsByCampusId(id)) {
            throw new IllegalStateException("Cannot delete campus that contains existing departments");
        }
        campusRepository.delete(campus);
    }

    @Transactional(readOnly = true)
    public CampusResponse getCampusById(Long id) {
        return mapToResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CampusResponse> getAllCampuses() {
        return campusRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampusResponse> getActiveCampuses() {
        return campusRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Campus findEntityById(Long id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campus not found with ID: " + id));
    }

    private CampusResponse mapToResponse(Campus c) {
        return new CampusResponse(
                c.getId(),
                c.getCode(),
                c.getChedInstitutionalCode(),
                c.getName(),
                c.getAddress(),
                c.getRegion(),
                c.getContactNumber(),
                c.getEmail(),
                c.isMain(),
                c.isActive()
        );
    }
}
