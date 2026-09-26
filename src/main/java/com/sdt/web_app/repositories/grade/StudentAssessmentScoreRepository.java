package com.sdt.web_app.repositories.grade;

import com.sdt.web_app.entities.grade.StudentAssessmentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAssessmentScoreRepository extends JpaRepository<StudentAssessmentScore, Long> {

    Optional<StudentAssessmentScore> findByItemIdAndStudentId(Long itemId, Long studentId);

    List<StudentAssessmentScore> findByStudentId(Long studentId);

    @Query("SELECT sas FROM StudentAssessmentScore sas JOIN FETCH sas.item cri JOIN FETCH cri.category cat WHERE sas.student.id = :studentId")
    List<StudentAssessmentScore> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("SELECT sas FROM StudentAssessmentScore sas JOIN FETCH sas.item cri JOIN FETCH cri.category cat JOIN FETCH sas.student sp WHERE cat.config.section.id = :sectionId")
    List<StudentAssessmentScore> findBySectionId(@Param("sectionId") Long sectionId);

    void deleteByItemId(Long itemId);
}
