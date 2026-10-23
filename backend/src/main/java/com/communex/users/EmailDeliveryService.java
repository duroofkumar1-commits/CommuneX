
package com.communex.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final String host;
    private final String from;

    // Constructor
    public EmailDeliveryService(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String host,
            @Value("${communex.mail.from:noreply@communex.local}") String from
    ) {
        this.mailSender = mailSender;
        this.host = host == null ? "" : host.trim();
        this.from = from;
    }

    // Check whether email delivery is configured
    public boolean isConfigured() {
        return !host.isBlank();
    }

    // Send an email
    public boolean send(
            String to,
            String subject,
            String text
    ) {
        if (!isConfigured()) {
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);

        return true;
    }
}