package com.sdt.web_app.repositories.scheduling;

import com.sdt.web_app.entities.scheduling.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCampusIdAndIsActiveTrue(Long campusId);
    Optional<Room> findByCampusIdAndCode(Long campusId, String code);
    List<Room> findByIsActiveTrue();
}
