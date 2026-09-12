package com.sdt.web_app.repositories.compliance;

import com.sdt.web_app.entities.compliance.GraduationApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraduationApplicationRepository extends JpaRepository<GraduationApplication, Long> {

    @Query("SELECT g FROM GraduationApplication g WHERE g.studentProfile.id = :studentProfileId ORDER BY g.createdAt DESC")
    List<GraduationApplication> findByStudentProfileId(@Param("studentProfileId") Long studentProfileId);

    @Query("SELECT g FROM GraduationApplication g WHERE g.studentProfile.id = :studentProfileId AND g.term.id = :termId")
    Optional<GraduationApplication> findByStudentProfileIdAndTermId(
            @Param("studentProfileId") Long studentProfileId,
            @Param("termId") Long termId
    );

    @Query("SELECT g FROM GraduationApplication g WHERE g.term.id = :termId")
    List<GraduationApplication> findByTermId(@Param("termId") Long termId);
}
