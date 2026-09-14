package com.sdt.web_app.repositories.grade;

import com.sdt.web_app.entities.grade.GradeSealingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeSealingAuditRepository extends JpaRepository<GradeSealingAudit, Long> {
    List<GradeSealingAudit> findBySectionId(Long sectionId);
}
