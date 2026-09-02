package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.GradingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, Long> {

    Optional<GradingScale> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT g FROM GradingScale g WHERE g.isNonNumeric = false AND :percentage BETWEEN g.percentageMin AND g.percentageMax")
    Optional<GradingScale> findTransmutationScaleForPercentage(@Param("percentage") BigDecimal percentage);

    List<GradingScale> findByIsPassingTrue();

    List<GradingScale> findByIsNonNumericTrue();

    List<GradingScale> findAllByOrderByPercentageMinDesc();
}
