package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.FeeCatalog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeCatalogRepository extends JpaRepository<FeeCatalog, Long> {

    Optional<FeeCatalog> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"category"})
    List<FeeCatalog> findByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    List<FeeCatalog> findByIsFheBillableTrue();

    @EntityGraph(attributePaths = {"category"})
    List<FeeCatalog> findByIsChedSanctionedTrue();
}
