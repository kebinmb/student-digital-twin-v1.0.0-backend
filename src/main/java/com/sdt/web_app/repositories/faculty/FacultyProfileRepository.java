package com.sdt.web_app.repositories.faculty;

import com.sdt.web_app.entities.faculty.FacultyProfile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyProfileRepository extends JpaRepository<FacultyProfile, Long>, JpaSpecificationExecutor<FacultyProfile> {

    @EntityGraph(attributePaths = {"user", "college", "program"})
    @Override
    List<FacultyProfile> findAll(Specification<FacultyProfile> spec);

    @EntityGraph(attributePaths = {"user", "college", "program"})
    Optional<FacultyProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "college", "program"})
    Optional<FacultyProfile> findByFacultyIdNumber(String facultyIdNumber);

    boolean existsByFacultyIdNumber(String facultyIdNumber);

    @Query("SELECT fp FROM FacultyProfile fp JOIN FETCH fp.user u LEFT JOIN FETCH fp.college c LEFT JOIN FETCH fp.program p")
    List<FacultyProfile> findAllWithUser();

    @Query("SELECT fp FROM FacultyProfile fp JOIN FETCH fp.user u LEFT JOIN FETCH fp.college c LEFT JOIN FETCH fp.program p WHERE fp.user.id = :userId")
    Optional<FacultyProfile> findByUserIdWithUser(@Param("userId") Long userId);
}
