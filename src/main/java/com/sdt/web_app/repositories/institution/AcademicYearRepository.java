package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    Optional<AcademicYear> findByCode(String code);

    boolean existsByCode(String code);

    Optional<AcademicYear> findByIsCurrentTrue();

    List<AcademicYear> findAllByOrderByStartDateDesc();
}
