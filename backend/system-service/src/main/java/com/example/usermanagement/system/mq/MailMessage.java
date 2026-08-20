package com.example.usermanagement.system.mq;

import java.io.Serializable;
import java.util.List;

public record MailMessage(
        Long notificationId,
        String subject,
        String content,
        List<String> recipients,
        String createdAt) implements Serializable {
}
