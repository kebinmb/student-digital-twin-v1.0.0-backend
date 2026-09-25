package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long>, JpaSpecificationExecutor<Major> {
    List<Major> findByProgramId(Long programId);
    List<Major> findByProgramIdAndIsActiveTrue(Long programId);
    Optional<Major> findByProgramIdAndCode(Long programId, String code);
    boolean existsByProgramIdAndCode(Long programId, String code);
}
