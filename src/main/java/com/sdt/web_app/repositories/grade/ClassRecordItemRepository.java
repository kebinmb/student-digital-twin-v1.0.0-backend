package com.sdt.web_app.repositories.grade;

import com.sdt.web_app.entities.grade.ClassRecordItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRecordItemRepository extends JpaRepository<ClassRecordItem, Long> {

    List<ClassRecordItem> findByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT cri FROM ClassRecordItem cri JOIN FETCH cri.category cat WHERE cat.config.section.id = :sectionId ORDER BY cri.sequenceOrder ASC, cri.id ASC")
    List<ClassRecordItem> findBySectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT COUNT(cri) > 0 FROM ClassRecordItem cri WHERE cri.category.id = :categoryId AND LOWER(TRIM(cri.itemTitle)) = LOWER(TRIM(:itemTitle))")
    boolean existsByCategoryIdAndItemTitleIgnoreCaseTrimmed(@Param("categoryId") Long categoryId, @Param("itemTitle") String itemTitle);

    @Query("SELECT cri.category.config.section.id FROM ClassRecordItem cri WHERE cri.id = :itemId")
    java.util.Optional<Long> findSectionIdByItemId(@Param("itemId") Long itemId);
}
