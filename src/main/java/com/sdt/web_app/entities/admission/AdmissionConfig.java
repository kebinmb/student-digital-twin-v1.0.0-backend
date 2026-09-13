package com.sdt.web_app.entities.admission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "admission_configs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class AdmissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false, unique = true)
    @ToString.Exclude
    @JsonIgnore
    private Term term;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = false;

    @Column(name = "daily_slot_limit", nullable = false)
    @Builder.Default
    private int dailySlotLimit = 1000;

    @Column(name = "total_opened_slots", nullable = false)
    @Builder.Default
    private int totalOpenedSlots = 20000;

    @Column(name = "days_open", nullable = false)
    @Builder.Default
    private int daysOpen = 20;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void recalculateDaysOpen() {
        if (dailySlotLimit > 0) {
            this.daysOpen = (int) Math.ceil((double) totalOpenedSlots / dailySlotLimit);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AdmissionConfig that = (AdmissionConfig) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
