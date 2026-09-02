package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.ScholarshipCategory;
import com.sdt.web_app.entities.institution.ScholarshipDiscount;
import com.sdt.web_app.entities.institution.ScholarshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScholarshipDiscountRepository extends JpaRepository<ScholarshipDiscount, Long> {

    Optional<ScholarshipDiscount> findByCode(String code);

    boolean existsByCode(String code);

    List<ScholarshipDiscount> findByType(ScholarshipType type);

    List<ScholarshipDiscount> findByCategory(ScholarshipCategory category);

    List<ScholarshipDiscount> findByAppliesToTuitionTrue();
}
