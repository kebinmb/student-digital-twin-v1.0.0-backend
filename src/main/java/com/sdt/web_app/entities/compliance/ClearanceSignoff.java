package com.sdt.web_app.entities.compliance;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clearance_signoffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceSignoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clearance_request_id", nullable = false)
    private ClearanceRequest clearanceRequest;

    @Column(name = "department_type", nullable = false, length = 50)
    private String departmentType;

    @Column(name = "signoff_status", nullable = false, length = 30)
    @Builder.Default
    private String signoffStatus = "PENDING";

    @Column(name = "remarks", length = 255)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_user_id")
    private User signedByUser;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;
}
