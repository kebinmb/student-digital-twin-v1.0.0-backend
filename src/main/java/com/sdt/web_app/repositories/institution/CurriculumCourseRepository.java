package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.CurriculumCourse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumCourseRepository extends JpaRepository<CurriculumCourse, Long> {

    Optional<CurriculumCourse> findByCurriculumIdAndCourseId(Long curriculumId, Long courseId);

    boolean existsByCurriculumIdAndCourseId(Long curriculumId, Long courseId);

    boolean existsByCourseId(Long courseId);

    @EntityGraph(attributePaths = {"curriculum", "course"})
    List<CurriculumCourse> findByCurriculumId(Long curriculumId);

    @EntityGraph(attributePaths = {"curriculum", "course"})
    List<CurriculumCourse> findByCurriculumIdAndYearLevelAndSemester(Long curriculumId, int yearLevel, String semester);
}
