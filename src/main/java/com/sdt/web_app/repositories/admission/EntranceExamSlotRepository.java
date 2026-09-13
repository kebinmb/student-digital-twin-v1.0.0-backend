package com.sdt.web_app.repositories.admission;

import com.sdt.web_app.entities.admission.EntranceExamSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntranceExamSlotRepository extends JpaRepository<EntranceExamSlot, Long> {

    List<EntranceExamSlot> findByTermIdAndStatusOrderByExamDateAscStartTimeAsc(Long termId, EntranceExamSlot.SlotStatus status);

    List<EntranceExamSlot> findByTermId(Long termId);

    @Modifying
    @Query("UPDATE EntranceExamSlot s SET s.reservedCount = s.reservedCount + 1, " +
           "s.status = CASE WHEN (s.reservedCount + 1) >= s.maxCapacity THEN com.sdt.web_app.entities.admission.EntranceExamSlot$SlotStatus.FULL ELSE s.status END " +
           "WHERE s.id = :slotId AND s.reservedCount < s.maxCapacity AND s.status = com.sdt.web_app.entities.admission.EntranceExamSlot$SlotStatus.OPEN")
    int incrementReservedCountIfOpen(@Param("slotId") Long slotId);
}
