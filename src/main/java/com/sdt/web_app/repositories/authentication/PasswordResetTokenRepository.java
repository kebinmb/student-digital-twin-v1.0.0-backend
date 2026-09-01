package com.sdt.web_app.repositories.authentication;

import com.sdt.web_app.entities.authentication.PasswordResetToken;
import com.sdt.web_app.entities.authentication.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}
