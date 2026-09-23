package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "or_booklets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrBooklet {

    public enum BookletStatus {
        ACTIVE, EXHAUSTED, RECALLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booklet_code", nullable = false, unique = true, length = 50)
    private String bookletCode;

    @Column(name = "start_or_number", nullable = false, length = 30)
    private String startOrNumber;

    @Column(name = "end_or_number", nullable = false, length = 30)
    private String endOrNumber;

    @Column(name = "current_or_number", nullable = false, length = 30)
    private String currentOrNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_cashier_id", nullable = false)
    private User assignedCashier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookletStatus status = BookletStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
