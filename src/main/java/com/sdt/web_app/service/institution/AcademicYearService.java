package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.AcademicYearDtos.*;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;

    public AcademicYearResponse createAcademicYear(CreateAcademicYearRequest request) {
        if (academicYearRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Academic year with code already exists: " + request.code());
        }
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (request.isCurrent()) {
            academicYearRepository.findByIsCurrentTrue().ifPresent(AcademicYear::unmarkAsCurrent);
        }

        AcademicYear academicYear = AcademicYear.builder()
                .code(request.code().trim())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isCurrent(request.isCurrent())
                .build();

        AcademicYear saved = academicYearRepository.save(academicYear);
        return mapToResponse(saved);
    }

    public AcademicYearResponse updateAcademicYear(Long id, UpdateAcademicYearRequest request) {
        AcademicYear academicYear = findEntityById(id);
        academicYear.updateDates(request.startDate(), request.endDate());
        return mapToResponse(academicYear);
    }

    @Transactional
    public AcademicYearResponse setCurrentAcademicYear(Long id) {
        AcademicYear targetYear = findEntityById(id);
        if (!targetYear.isCurrent()) {
            academicYearRepository.findAllByIsCurrentTrue().forEach(AcademicYear::unmarkAsCurrent);
            targetYear.markAsCurrent();
        }
        return mapToResponse(targetYear);
    }

    public void deleteAcademicYear(Long id) {
        AcademicYear academicYear = findEntityById(id);
        if (termRepository.existsByAcademicYearId(id)) {
            throw new IllegalStateException("Cannot delete academic year referenced by academic terms");
        }
        academicYearRepository.delete(academicYear);
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getAcademicYearById(Long id) {
        return mapToResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getCurrentAcademicYear() {
        return academicYearRepository.findFirstByIsCurrentTrueOrderByIdDesc()
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("No active current academic year designated"));
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> getAllAcademicYears() {
        return academicYearRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AcademicYear findEntityById(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found with ID: " + id));
    }

    private AcademicYearResponse mapToResponse(AcademicYear ay) {
        return new AcademicYearResponse(
                ay.getId(),
                ay.getCode(),
                ay.getStartDate(),
                ay.getEndDate(),
                ay.isCurrent()
        );
    }
}
