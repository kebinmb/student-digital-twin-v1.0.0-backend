package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {

    Optional<Campus> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Campus> findByIsMainTrue();

    List<Campus> findByIsActiveTrue();

    List<Campus> findByRegion(String region);
}
