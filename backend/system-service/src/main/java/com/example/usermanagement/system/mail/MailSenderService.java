package com.example.usermanagement.system.mail;

import com.example.usermanagement.system.mq.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailSenderService {
    private static final Logger log = LoggerFactory.getLogger(MailSenderService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean sendEnabled;

    public MailSenderService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.from:${spring.mail.username:no-reply@risk.local}}") String from,
            @Value("${app.mail.send-enabled:false}") boolean sendEnabled) {
        this.mailSender = mailSender.getIfAvailable();
        this.from = from;
        this.sendEnabled = sendEnabled;
    }

    public void send(MailMessage message) {
        if (message.recipients() == null || message.recipients().isEmpty()) {
            log.warn("Skip mail notification {}, recipients are empty", message.notificationId());
            return;
        }
        if (mailSender == null) {
            log.warn("JavaMailSender is not available, skip mail notification {}", message.notificationId());
            return;
        }
        if (!sendEnabled) {
            log.info("Mail send is disabled, consume MQ mail notification only, notificationId={}, recipients={}, subject={}",
                    message.notificationId(), message.recipients().size(), message.subject());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.recipients().toArray(String[]::new));
        mail.setSubject(message.subject());
        mail.setText(message.content() == null ? "" : message.content());
        mailSender.send(mail);
    }
}
