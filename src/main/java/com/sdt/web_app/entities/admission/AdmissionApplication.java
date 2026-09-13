package com.sdt.web_app.entities.admission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "admission_applications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class AdmissionApplication {

    public enum ApplicationStatus {
        SUBMITTED,
        UNDER_REVIEW,
        EXAM_PASSED,
        EXAM_FAILED,
        INTERVIEW_ACCEPTED,
        REJECTED,
        ELIGIBLE_FOR_ENROLLMENT,
        ENROLLED,
        APPROVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, unique = true, length = 30)
    private String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_program_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Program targetProgram;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_slot_id")
    @ToString.Exclude
    @JsonIgnore
    private EntranceExamSlot examSlot;

    // Personal Data
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "suffix", length = 10)
    private String suffix;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "birth_place", length = 150)
    private String birthPlace;

    @Column(name = "gender", nullable = false, length = 20)
    @Builder.Default
    private String gender = "FEMALE";

    @Column(name = "gender_identity", length = 30)
    private String genderIdentity;

    @Column(name = "civil_status", nullable = false, length = 20)
    @Builder.Default
    private String civilStatus = "SINGLE";

    @Column(name = "citizenship", nullable = false, length = 50)
    @Builder.Default
    private String citizenship = "FILIPINO";

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    // Academic Background & Form 137 Details
    @Column(name = "lrn_number", length = 30)
    private String lrnNumber;

    @Column(name = "high_school_name", nullable = false, length = 150)
    private String highSchoolName;

    @Column(name = "deped_school_id", length = 30)
    private String depedSchoolId;

    @Column(name = "high_school_type", nullable = false, length = 30)
    @Builder.Default
    private String highSchoolType = "PUBLIC";

    @Column(name = "shs_track_and_strand", length = 100)
    private String shsTrackAndStrand;

    @Column(name = "high_school_gwa", precision = 4, scale = 2)
    private BigDecimal highSchoolGwa;

    @Column(name = "shs_year_graduated")
    private Integer shsYearGraduated;

    // Address
    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @Column(name = "barangay", nullable = false, length = 100)
    private String barangay;

    @Column(name = "city_municipality", nullable = false, length = 100)
    private String cityMunicipality;

    @Column(name = "province", nullable = false, length = 100)
    private String province;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    // Permanent Address Fields
    @Column(name = "perm_region", length = 100)
    private String permRegion;

    @Column(name = "perm_province", length = 100)
    private String permProvince;

    @Column(name = "perm_city_municipality", length = 100)
    private String permCityMunicipality;

    @Column(name = "perm_barangay", length = 100)
    private String permBarangay;

    @Column(name = "perm_zip_code", length = 10)
    private String permZipCode;

    @Column(name = "perm_street_address", length = 255)
    private String permStreetAddress;

    // Emergency Contact
    @Column(name = "emergency_contact_name", nullable = false, length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_relationship", nullable = false, length = 50)
    private String emergencyContactRelationship;

    @Column(name = "emergency_contact_number", nullable = false, length = 20)
    private String emergencyContactNumber;

    @Column(name = "emergency_contact_email", length = 100)
    private String emergencyContactEmail;

    // Philippine Statutory Equity Indicators
    @Column(name = "is_4ps_beneficiary", nullable = false)
    @Builder.Default
    private boolean is4psBeneficiary = false;

    @Column(name = "household_4ps_id_number", length = 60)
    private String household4psIdNumber;

    @Column(name = "is_indigenous_people", nullable = false)
    @Builder.Default
    private boolean isIndigenousPeople = false;

    @Column(name = "ip_ethnic_group", length = 100)
    private String ipEthnicGroup;

    @Column(name = "ncip_certificate_number", length = 100)
    private String ncipCertificateNumber;

    @Column(name = "is_person_with_disability", nullable = false)
    @Builder.Default
    private boolean isPersonWithDisability = false;

    @Column(name = "disability_type", length = 60)
    private String disabilityType;

    @Column(name = "pwd_id_number", length = 60)
    private String pwdIdNumber;

    @Column(name = "is_solo_parent_or_dependent", nullable = false)
    @Builder.Default
    private boolean isSoloParentOrDependent = false;

    @Column(name = "solo_parent_id_number", length = 60)
    private String soloParentIdNumber;

    @Column(name = "is_underprivileged_homeless", nullable = false)
    @Builder.Default
    private boolean isUnderprivilegedHomeless = false;

    @Column(name = "is_displaced_or_rebel_returnee", nullable = false)
    @Builder.Default
    private boolean isDisplacedOrRebelReturnee = false;

    @Column(name = "monthly_household_income_bracket", nullable = false, length = 50)
    @Builder.Default
    private String monthlyHouseholdIncomeBracket = "POOR_BELOW_10K";

    @Column(name = "scholarship_grant_type", length = 60)
    private String scholarshipGrantType;

    // Queue & Concurrency Management Token
    @Column(name = "queue_token", length = 100)
    private String queueToken;

    @Column(name = "queue_position")
    private Integer queuePosition;

    // Evaluation & Interview Workflow
    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus applicationStatus = ApplicationStatus.SUBMITTED;

    @Column(name = "exam_score", precision = 5, scale = 2)
    private BigDecimal examScore;

    @Column(name = "exam_remarks", length = 255)
    private String examRemarks;

    @Column(name = "interview_score", precision = 5, scale = 2)
    private BigDecimal interviewScore;

    @Column(name = "interview_remarks", length = 255)
    private String interviewRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluated_by_user_id")
    @ToString.Exclude
    @JsonIgnore
    private User evaluatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewed_by_user_id")
    @ToString.Exclude
    @JsonIgnore
    private User interviewedBy;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (middleName != null && !middleName.isBlank()) sb.append(" ").append(middleName);
        if (lastName != null) sb.append(" ").append(lastName);
        if (suffix != null && !suffix.isBlank()) sb.append(" ").append(suffix);
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AdmissionApplication that = (AdmissionApplication) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
