


package com.example.backend.service;

import com.example.backend.config.AppProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mail;
    private final AppProperties props;

    public EmailService(JavaMailSender mail, AppProperties props) {
        this.mail = mail;
        this.props = props;
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Verify your email");
        msg.setText("Click the link below to verify your account:\n\n" + buildVerifyLink(token)
                + "\n\nThis link expires in " + props.getVerification().getTtlMinutes() + " minutes.");
        mail.send(msg);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Reset your password");
        msg.setText("Click the link below to reset your password:\n\n" + buildResetLink(token)
                + "\n\nThis link expires in " + props.getReset().getTtlMinutes() + " minutes."
                + "\n\nIf you did not request a password reset, please ignore this email.");
        mail.send(msg);
    }

       private String buildVerifyLink(String token) {
        if (props.getEmailLinks().isFrontendEnabled()) {
            return props.getFrontendBaseUrl() + "/login?verifyToken=" + token;
        } else {
            return props.getBackendBaseUrl() + "/api/auth/verify?token=" + token;
        }
    }



    private String buildResetLink(String token) {
        if (props.getEmailLinks().isFrontendEnabled()) {
            // Go straight to Angular — cleanest option, no extra redirect
            return props.getFrontendBaseUrl() + "/reset-password?token=" + token;
        } else {
            // Goes to backend GET endpoint which redirects to Angular
            return props.getBackendBaseUrl() + "/api/auth/reset-password?token=" + token;
        }
    }
}