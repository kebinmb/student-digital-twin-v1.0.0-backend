package com.sdt.web_app.entities.faculty;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "faculty_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"user", "college", "program"})
public class FacultyProfile {

    public enum HighestDegree {
        BACHELORS, MASTERS, DOCTORATE, POST_DOCTORATE
    }

    public enum AcademicRank {
        INSTRUCTOR_I, INSTRUCTOR_II, INSTRUCTOR_III,
        ASSISTANT_PROFESSOR_I, ASSISTANT_PROFESSOR_II, ASSISTANT_PROFESSOR_III, ASSISTANT_PROFESSOR_IV,
        ASSOCIATE_PROFESSOR_I, ASSOCIATE_PROFESSOR_II, ASSOCIATE_PROFESSOR_III, ASSOCIATE_PROFESSOR_IV,
        PROFESSOR_I, PROFESSOR_II, PROFESSOR_III, PROFESSOR_IV, PROFESSOR_V, PROFESSOR_VI,
        UNIVERSITY_PROFESSOR
    }

    public enum EmploymentStatus {
        FULL_TIME, PART_TIME, ADJUNCT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private Department college;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(name = "faculty_id_number", nullable = false, unique = true, length = 30)
    private String facultyIdNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "highest_degree", nullable = false, length = 50)
    @Builder.Default
    private HighestDegree highestDegree = HighestDegree.BACHELORS;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_rank", nullable = false, length = 50)
    @Builder.Default
    private AcademicRank academicRank = AcademicRank.INSTRUCTOR_I;

    @Column(name = "prc_license_no", length = 50)
    private String prcLicenseNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.FULL_TIME;

    @Column(name = "is_tenured", nullable = false)
    @Builder.Default
    private boolean isTenured = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateCredentials(
            HighestDegree highestDegree,
            AcademicRank academicRank,
            String prcLicenseNo,
            EmploymentStatus employmentStatus,
            boolean isTenured) {
        if (highestDegree != null) this.highestDegree = highestDegree;
        if (academicRank != null) this.academicRank = academicRank;
        this.prcLicenseNo = prcLicenseNo;
        if (employmentStatus != null) this.employmentStatus = employmentStatus;
        this.isTenured = isTenured;
    }

    public void assignCollege(Department college) {
        this.college = college;
    }

    public void assignProgram(Program program) {
        this.program = program;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FacultyProfile that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
