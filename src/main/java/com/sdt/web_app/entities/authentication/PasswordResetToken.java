package com.sdt.web_app.entities.authentication;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "user")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id", foreignKey = @ForeignKey(name = "fk_password_reset_user"))
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public static PasswordResetToken createTokenForUser(String token, User user, int expirationMinutes) {
        return PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES))
                .build();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken that)) return false;
        return token != null && token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
