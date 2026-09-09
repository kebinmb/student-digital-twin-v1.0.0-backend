package com.sdt.web_app.repositories.grade;

import com.sdt.web_app.entities.grade.SectionGradingCategory;
import com.sdt.web_app.entities.grade.SectionGradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionGradingConfigRepository extends JpaRepository<SectionGradingConfig, Long> {

    Optional<SectionGradingConfig> findBySectionId(Long sectionId);

    @Query("SELECT DISTINCT cfg FROM SectionGradingConfig cfg LEFT JOIN FETCH cfg.categories WHERE cfg.section.id = :sectionId")
    Optional<SectionGradingConfig> findBySectionIdWithCategories(@Param("sectionId") Long sectionId);

    @Query("SELECT DISTINCT cat FROM SectionGradingCategory cat LEFT JOIN FETCH cat.items WHERE cat.config.id = :configId")
    List<SectionGradingCategory> findCategoriesWithItemsByConfigId(@Param("configId") Long configId);

    default Optional<SectionGradingConfig> findBySectionIdWithDetails(Long sectionId) {
        Optional<SectionGradingConfig> configOpt = findBySectionIdWithCategories(sectionId);
        if (configOpt.isPresent()) {
            findCategoriesWithItemsByConfigId(configOpt.get().getId());
        }
        return configOpt;
    }

    boolean existsBySectionId(Long sectionId);
}

