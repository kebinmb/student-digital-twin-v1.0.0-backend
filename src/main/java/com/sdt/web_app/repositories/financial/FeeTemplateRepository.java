package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.FeeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FeeTemplateRepository extends JpaRepository<FeeTemplate, Long> {

    @Query("SELECT f FROM FeeTemplate f WHERE f.academicYear.id = :academicYearId AND f.active = true ORDER BY f.createdAt DESC")
    List<FeeTemplate> findActiveByAcademicYear(Long academicYearId);

    Optional<FeeTemplate> findFirstByAcademicYearIdAndActiveTrueOrderByCreatedAtDesc(Long academicYearId);
}
