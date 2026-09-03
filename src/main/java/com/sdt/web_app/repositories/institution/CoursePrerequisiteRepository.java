package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.CoursePrerequisite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    Optional<CoursePrerequisite> findByCourseIdAndPrerequisiteCourseId(Long courseId, Long prerequisiteCourseId);

    boolean existsByCourseIdAndPrerequisiteCourseId(Long courseId, Long prerequisiteCourseId);

    @EntityGraph(attributePaths = {"course", "prerequisiteCourse"})
    List<CoursePrerequisite> findByCourseId(Long courseId);

    @EntityGraph(attributePaths = {"course", "prerequisiteCourse"})
    List<CoursePrerequisite> findByPrerequisiteCourseId(Long prerequisiteCourseId);

    boolean existsByCourseIdOrPrerequisiteCourseId(Long courseId, Long prerequisiteCourseId);

    @org.springframework.data.jpa.repository.Query("SELECT cp FROM CoursePrerequisite cp JOIN FETCH cp.prerequisiteCourse WHERE cp.course.id IN :courseIds")
    List<CoursePrerequisite> findPrerequisitesForCourseIds(@org.springframework.data.repository.query.Param("courseIds") java.util.Collection<Long> courseIds);
}
