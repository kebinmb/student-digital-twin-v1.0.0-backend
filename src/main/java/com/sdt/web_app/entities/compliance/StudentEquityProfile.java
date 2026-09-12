package com.sdt.web_app.entities.compliance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_equity_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@BatchSize(size = 50)
public class StudentEquityProfile {

    public enum DisabilityType {
        VISUAL, HEARING, MOBILITY, NEURODEVELOPMENTAL, PSYCHOSOCIAL, CHRONIC_ILLNESS, OTHER
    }

    public enum HouseholdIncomeBracket {
        POOR_BELOW_10K,
        LOW_INCOME_10K_TO_20K,
        LOWER_MIDDLE_20K_TO_40K,
        MIDDLE_40K_TO_70K,
        UPPER_70K_PLUS
    }

    public enum EquityVerificationStatus {
        SELF_DECLARED, DOCUMENTED, VERIFIED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false, unique = true)
    @ToString.Exclude
    @JsonIgnore
    private StudentProfile studentProfile;

    // RA 10931 / DSWD Classifications
    @Column(name = "is_4ps_beneficiary", nullable = false)
    @Builder.Default
    private Boolean is4psBeneficiary = false;

    @Column(name = "household_4ps_id_number", length = 60)
    private String household4psIdNumber;

    @Column(name = "is_listahanan_nhts", nullable = false)
    @Builder.Default
    private Boolean isListahananNhts = false;

    @Column(name = "unifast_tes_awardee", nullable = false)
    @Builder.Default
    private Boolean unifastTesAwardee = false;

    @Column(name = "unifast_tes_award_number", length = 60)
    private String unifastTesAwardNumber;

    // RA 8371 (Indigenous Peoples)
    @Column(name = "is_indigenous_people", nullable = false)
    @Builder.Default
    private Boolean isIndigenousPeople = false;

    @Column(name = "ip_ethnic_group", length = 100)
    private String ipEthnicGroup;

    @Column(name = "ncip_certificate_number", length = 100)
    private String ncipCertificateNumber;

    // RA 7277 / RA 9442 (PWD)
    @Column(name = "is_person_with_disability", nullable = false)
    @Builder.Default
    private Boolean isPersonWithDisability = false;

    @Column(name = "pwd_id_number", length = 60)
    private String pwdIdNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "disability_type", length = 60)
    private DisabilityType disabilityType;

    // RA 11861 (Solo Parents)
    @Column(name = "is_solo_parent_or_dependent", nullable = false)
    @Builder.Default
    private Boolean isSoloParentOrDependent = false;

    @Column(name = "solo_parent_id_number", length = 60)
    private String soloParentIdNumber;

    // Socioeconomic & Demographic Equity Targets
    @Column(name = "is_first_generation_college", nullable = false)
    @Builder.Default
    private Boolean isFirstGenerationCollege = false;

    @Column(name = "is_gida_resident", nullable = false)
    @Builder.Default
    private Boolean isGidaResident = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "monthly_household_income_bracket", nullable = false, length = 40)
    @Builder.Default
    private HouseholdIncomeBracket monthlyHouseholdIncomeBracket = HouseholdIncomeBracket.POOR_BELOW_10K;

    // Verification & Governance
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private EquityVerificationStatus verificationStatus = EquityVerificationStatus.SELF_DECLARED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    @ToString.Exclude
    @JsonIgnore
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_remarks", length = 255)
    private String verificationRemarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
