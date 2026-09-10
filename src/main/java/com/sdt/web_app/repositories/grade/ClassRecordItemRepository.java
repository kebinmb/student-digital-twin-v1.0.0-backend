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

    @Query("SELECT cri FROM ClassRecordItem cri JOIN FETCH cri.category cat WHERE cat.config.section.id = :sectionId")
    List<ClassRecordItem> findBySectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT cri.category.config.section.id FROM ClassRecordItem cri WHERE cri.id = :itemId")
    java.util.Optional<Long> findSectionIdByItemId(@Param("itemId") Long itemId);
}
