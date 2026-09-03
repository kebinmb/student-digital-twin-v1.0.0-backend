package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.CourseOutcome;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseOutcomeRepository extends JpaRepository<CourseOutcome, Long> {

    Optional<CourseOutcome> findByCourseIdAndCode(Long courseId, String code);

    boolean existsByCourseIdAndCode(Long courseId, String code);

    @EntityGraph(attributePaths = {"course"})
    List<CourseOutcome> findByCourseId(Long courseId);

    List<CourseOutcome> findByBloomsLevel(String bloomsLevel);

    boolean existsByCourseId(Long courseId);
}
