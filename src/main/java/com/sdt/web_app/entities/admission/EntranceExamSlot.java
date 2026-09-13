package com.sdt.web_app.entities.admission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "entrance_exam_slots", uniqueConstraints = {
    @UniqueConstraint(name = "uq_term_exam_date_time_venue", columnNames = {"term_id", "exam_date", "start_time", "venue_room"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class EntranceExamSlot {

    public enum SlotStatus {
        OPEN, FULL, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Term term;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "venue_room", nullable = false, length = 100)
    private String venueRoom;

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private int maxCapacity = 50;

    @Column(name = "reserved_count", nullable = false)
    @Builder.Default
    private int reservedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SlotStatus status = SlotStatus.OPEN;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public int getAvailableSeats() {
        return Math.max(0, maxCapacity - reservedCount);
    }

    public boolean isFull() {
        return reservedCount >= maxCapacity || status == SlotStatus.FULL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        EntranceExamSlot that = (EntranceExamSlot) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
