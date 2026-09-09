package com.sdt.web_app.repositories.grade;

import com.sdt.web_app.entities.grade.SectionGradingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionGradingCategoryRepository extends JpaRepository<SectionGradingCategory, Long> {

    List<SectionGradingCategory> findByConfigId(Long configId);

    @Query("SELECT DISTINCT cat FROM SectionGradingCategory cat LEFT JOIN FETCH cat.items WHERE cat.config.id = :configId")
    List<SectionGradingCategory> findCategoriesWithItemsByConfigId(@Param("configId") Long configId);

    void deleteByConfigId(Long configId);
}

