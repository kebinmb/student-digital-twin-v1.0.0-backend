package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.Program;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long>, JpaSpecificationExecutor<Program> {

    Optional<Program> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"department"})
    List<Program> findByDepartmentId(Long departmentId);

    boolean existsByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"department"})
    List<Program> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"department"})
    List<Program> findByChairpersonUserId(Long userId);

    @EntityGraph(attributePaths = {"department"})
    Optional<Program> findFirstByChairpersonUserId(Long userId);

    @EntityGraph(attributePaths = {"department", "college"})
    List<Program> findByDepartmentIdIn(List<Long> departmentIds);

    @EntityGraph(attributePaths = {"department", "college"})
    List<Program> findByCollegeId(Long collegeId);

    @EntityGraph(attributePaths = {"department", "college"})
    List<Program> findByCollegeIdAndIsActiveTrue(Long collegeId);
}
