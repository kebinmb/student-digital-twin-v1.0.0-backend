package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.FeeCategory;
import com.sdt.web_app.entities.institution.FeeCategoryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {

    Optional<FeeCategory> findByCode(FeeCategoryCode code);

    boolean existsByCode(FeeCategoryCode code);
}
