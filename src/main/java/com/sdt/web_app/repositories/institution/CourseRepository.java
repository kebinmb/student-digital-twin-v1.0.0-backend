package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    List<Course> findByIsActiveTrue();

    Page<Course> findByIsActiveTrue(Pageable pageable);

    Page<Course> findByIsActiveTrueAndIdNotIn(Collection<Long> assignedIds, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.isActive = true AND c.id NOT IN :assignedIds AND (:search IS NULL OR :search = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Course> findAvailableCoursesExcluding(@Param("assignedIds") Collection<Long> assignedIds, @Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.isActive = true AND (:search IS NULL OR :search = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Course> findAvailableCoursesAll(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.isActive = true AND (:search IS NULL OR :search = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Course> searchCourses(@Param("search") String search, Pageable pageable);
}
