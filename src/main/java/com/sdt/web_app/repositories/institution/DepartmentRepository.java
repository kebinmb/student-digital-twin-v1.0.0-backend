package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.DepartmentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCampusIdAndCode(Long campusId, String code);

    boolean existsByCampusIdAndCode(Long campusId, String code);

    @EntityGraph(attributePaths = {"campus", "parentDepartment"})
    Optional<Department> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"campus"})
    List<Department> findByCampusId(Long campusId);

    List<Department> findByParentDepartmentId(Long parentDepartmentId);

    List<Department> findByType(DepartmentType type);

    List<Department> findByIsActiveTrue();
}
