// File: com/sdt/web_app/service/authentication/PasswordResetService.java

package com.sdt.web_app.service.authentication;

import com.sdt.web_app.entities.authentication.PasswordResetToken;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.PasswordResetTokenRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int EXPIRATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username:no-reply@chmsu.edu.ph}")
    private String fromEmail;

    public record PasswordResetResult(String username, String email) {}

    @Transactional
    public void initiatePasswordReset(String email, String frontendUrl) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            String rawToken = UUID.randomUUID().toString();
            String hashedToken = hashToken(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.createTokenForUser(hashedToken, user, EXPIRATION_MINUTES);
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            sendResetEmail(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public PasswordResetResult completePasswordReset(String rawToken, String newRawPassword) {
        String hashedToken = hashToken(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or non-existent token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        String hashedPassword = passwordEncoder.encode(newRawPassword);
        user.updatePassword(hashedPassword);
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        log.info("Password successfully updated for user: {}", user.getUsername());

        return new PasswordResetResult(user.getUsername(), user.getEmail());
    }

    public void sendResetEmail(String toEmail, String resetLink) {
        log.info("Preparing password reset email dispatch for: {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request - Student Digital Twin");
            message.setText(
                    "Hello,\n\n" +
                            "You requested a password reset for your account. Please click the link below to set a new password:\n\n" +
                            resetLink + "\n\n" +
                            "This link is valid for " + EXPIRATION_MINUTES + " minutes.\n" +
                            "If you did not request this, please ignore this email or contact ICT support.\n\n" +
                            "— CHMSU ICT Support"
            );

            javaMailSender.send(message);
            log.info("Password reset email successfully dispatched to: {}", toEmail);

        } catch (MailException e) {
            log.error("Failed to send password reset email to {}. Root cause: {}", toEmail, e.getMessage());
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM", e);
        }
    }
}