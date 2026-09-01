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

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public void initiatePasswordReset(String email, String frontendUrl) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.createTokenForUser(rawToken, user, EXPIRATION_MINUTES);
            passwordResetTokenRepository.save(resetToken);

            // Point to Angular Frontend route instead of Spring Boot API endpoint
            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            sendResetEmail(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public void completePasswordReset(String token, String newRawPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or non-existent token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        String hashedPassword = passwordEncoder.encode(newRawPassword);
        user.updatePassword(hashedPassword);
        userRepository.save(user);

        // Invalidate token upon successful reset
        passwordResetTokenRepository.delete(resetToken);
        log.info("Password successfully updated for user: {}", user.getUsername());
    }

    public void sendResetEmail(String toEmail, String resetLink) {
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

            log.info("Sending password reset email via SMTP host to: {}", toEmail);
            javaMailSender.send(message);
            log.info("Password reset email successfully dispatched to {}", toEmail);

        } catch (MailException e) {
            log.error("Failed to send password reset email to {}. Root cause: {}", toEmail, e.getMessage(), e);
            throw new IllegalStateException("Unable to send reset email at this time. Please try again later.");
        }
    }
}