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
        SELF_DECLARED, PENDING_VERIFICATION, VERIFIED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false, unique = true)
    @ToString.Exclude
    @JsonIgnore
    private StudentProfile studentProfile;

    // 1. RA 7277 / RA 9442 / RA 10754 (Person with Disability)
    @Column(name = "is_person_with_disability", nullable = false)
    @Builder.Default
    private Boolean isPersonWithDisability = false;

    @Column(name = "pwd_id_number", length = 60)
    private String pwdIdNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "disability_type", length = 60)
    private DisabilityType disabilityType;

    // 2. RA 8972 / RA 11861 (Solo Parent Status - Explicit Separation)
    @Column(name = "is_solo_parent", nullable = false)
    @Builder.Default
    private Boolean isSoloParent = false;

    @Column(name = "is_raised_by_solo_parent", nullable = false)
    @Builder.Default
    private Boolean isRaisedBySoloParent = false;

    @Column(name = "solo_parent_id_number", length = 60)
    private String soloParentIdNumber;

    // 3. RA 11310 (4Ps Beneficiary & UniFAST / DSWD)
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

    // 4. RA 8371 IPRA (Indigenous Peoples)
    @Column(name = "is_indigenous_people", nullable = false)
    @Builder.Default
    private Boolean isIndigenousPeople = false;

    @Column(name = "ip_ethnic_group", length = 100)
    private String ipEthnicGroup;

    @Column(name = "ncip_certificate_number", length = 100)
    private String ncipCertificateNumber;

    // 5. DSWD Case Study / Cert (Orphan Status)
    @Column(name = "is_orphan", nullable = false)
    @Builder.Default
    private Boolean isOrphan = false;

    // 6. DOH AO 2020-0023 (GIDA)
    @Column(name = "is_gida_resident", nullable = false)
    @Builder.Default
    private Boolean isGidaResident = false;

    @Column(name = "gida_barangay_residence", length = 150)
    private String gidaBarangayResidence;

    // 7. RA 8435 / RA 11321 (Subsistence Farmer or Fisherfolk)
    @Column(name = "is_farmer_fisherfolk", nullable = false)
    @Builder.Default
    private Boolean isFarmerFisherfolk = false;

    @Column(name = "rsbsa_registration_number", length = 60)
    private String rsbsaRegistrationNumber;

    // 8. EO 70 s. 2018 (Rebel Returnees / E-CLIP)
    @Column(name = "is_rebel_returnee_family", nullable = false)
    @Builder.Default
    private Boolean isRebelReturneeFamily = false;

    @Column(name = "certificate_of_surrender_number", length = 60)
    private String certificateOfSurrenderNumber;

    // 9. RA 10931 Sec 7/9 (Bottom 40% Household Income Bracket)
    @Column(name = "is_bottom_40_income_bracket", nullable = false)
    @Builder.Default
    private Boolean isBottom40IncomeBracket = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "monthly_household_income_bracket", nullable = false, length = 40)
    @Builder.Default
    private HouseholdIncomeBracket monthlyHouseholdIncomeBracket = HouseholdIncomeBracket.POOR_BELOW_10K;

    // 10. First-Generation College Student
    @Column(name = "is_first_generation_college", nullable = false)
    @Builder.Default
    private Boolean isFirstGenerationCollege = false;

    // 11. Verification & Audit Metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 25)
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
