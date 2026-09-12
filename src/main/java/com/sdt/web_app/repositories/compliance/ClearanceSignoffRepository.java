package com.sdt.web_app.repositories.compliance;

import com.sdt.web_app.entities.compliance.ClearanceSignoff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClearanceSignoffRepository extends JpaRepository<ClearanceSignoff, Long> {

    @Query("SELECT s FROM ClearanceSignoff s WHERE s.clearanceRequest.id = :requestId")
    List<ClearanceSignoff> findByClearanceRequestId(@Param("requestId") Long requestId);

    @Query("SELECT s FROM ClearanceSignoff s WHERE s.clearanceRequest.id = :requestId AND s.departmentType = :departmentType")
    Optional<ClearanceSignoff> findByClearanceRequestIdAndDepartmentType(
            @Param("requestId") Long requestId,
            @Param("departmentType") String departmentType
    );

    @Query("SELECT s FROM ClearanceSignoff s WHERE s.departmentType = :departmentType AND s.signoffStatus = :status")
    List<ClearanceSignoff> findByDepartmentTypeAndSignoffStatus(
            @Param("departmentType") String departmentType,
            @Param("status") String status
    );
}
