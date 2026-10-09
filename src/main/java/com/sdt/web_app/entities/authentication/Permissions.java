package com.sdt.web_app.entities.authentication;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class Permissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100, updatable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public static final String INTERVENTION_ACKNOWLEDGE = "intervention:telemetry:acknowledge";
    public static final String INTERVENTION_RESPOND = "intervention:telemetry:respond";
    public static final String EARLY_WARNING_RADAR_READ = "analytics:early-warning:read";
    public static final String GRADE_CHANGE_READ = "grades:change-requests:read";
    public static final String GRADE_CHANGE_APPROVE = "grades:change-requests:approve";
    public static final String CURRICULUM_READ = "curriculum:read";
    public static final String CAMPUS_READ = "campus:read";
    public static final String USER_READ = "users:read";
    public static final String SCHEDULE_READ = "scheduling:sections:read";
    public static final String ENROLLMENT_READ = "enrollment:read";
    public static final String UNIFAST_READ = "finance:unifast:read";
    public static final String UNIFAST_PROCESS = "finance:unifast:process";
    public static final String PAYMENT_READ = "finance:payment:read";
    public static final String PAYMENT_PROCESS = "finance:payment:process";
    public static final String EQUITY_PROFILE_READ = "compliance:equity:read";
    public static final String EQUITY_PROFILE_SEARCH = "compliance:equity:search";
    public static final String FINANCE_READ = "finance:read";

    public void updateDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permissions that)) return false;
        return name != null && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
