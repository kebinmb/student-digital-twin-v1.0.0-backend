package com.sdt.web_app.repositories.scheduling;

import com.sdt.web_app.entities.scheduling.FacultyWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyWorkloadRepository extends JpaRepository<FacultyWorkload, Long> {
    Optional<FacultyWorkload> findByTermIdAndFacultyId(Long termId, Long facultyUserId);
    List<FacultyWorkload> findByTermId(Long termId);
    boolean existsByTermIdAndFacultyId(Long termId, Long facultyUserId);
}
