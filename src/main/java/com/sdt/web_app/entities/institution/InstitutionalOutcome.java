package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "institutional_outcomes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class InstitutionalOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String statement;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public void update(String statement, String description, boolean active) {
        if (statement != null && !statement.isBlank()) {
            this.statement = statement;
        }
        this.description = description;
        this.active = active;
    }
}
