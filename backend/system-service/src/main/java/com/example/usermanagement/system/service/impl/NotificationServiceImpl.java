package com.example.usermanagement.system.service.impl;

import com.example.usermanagement.system.mapper.SystemCrudMapper;
import com.example.usermanagement.system.client.UserDirectoryClient;
import com.example.usermanagement.system.mq.MailMessage;
import com.example.usermanagement.system.mq.MailMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class NotificationServiceImpl extends AbstractSystemCrudService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final SystemCrudMapper mapper;
    private final MailMessagePublisher mailMessagePublisher;
    private final UserDirectoryClient userDirectoryClient;
    private final String internalServiceKey;

    public NotificationServiceImpl(
            SystemCrudMapper mapper,
            MailMessagePublisher mailMessagePublisher,
            UserDirectoryClient userDirectoryClient,
            @Value("${app.security.internal-service-key}") String internalServiceKey) {
        this.mapper = mapper;
        this.mailMessagePublisher = mailMessagePublisher;
        this.userDirectoryClient = userDirectoryClient;
        this.internalServiceKey = internalServiceKey;
    }

    @Override
    public Map<String, Object> create(Map<String, Object> body) {
        Map<String, Object> sourceBody = new LinkedHashMap<>(body);
        Map<String, Object> notification = super.create(body);
        publishMailIfNeeded(notification, sourceBody);
        return notification;
    }

    @Override
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Map<String, Object> sourceBody = new LinkedHashMap<>(body);
        Map<String, Object> notification = super.update(id, body);
        publishMailIfNeeded(notification, sourceBody);
        return notification;
    }

    @Override public Map<String, Object> get(Long id) { return mapper.getNotification(id); }
    @Override public void delete(Long id) { mapper.deleteNotification(id); }
    @Override protected List<Map<String, Object>> listRows(String keyword, int limit, int offset) { return mapper.listNotifications(keyword, limit, offset); }
    @Override protected long countRows(String keyword) { return mapper.countNotifications(keyword); }
    @Override protected void insert(Map<String, Object> body) { mapper.insertNotification(body); }
    @Override protected void updateRow(Long id, Map<String, Object> body) { mapper.updateNotification(id, body); }

    @Override
    protected void clean(Map<String, Object> body) {
        super.clean(body);
        body.remove("recipients");
        body.remove("target_email");
        body.remove("email");
    }

    private void publishMailIfNeeded(Map<String, Object> notification, Map<String, Object> sourceBody) {
        if (!"EMAIL".equalsIgnoreCase(String.valueOf(notification.get("channel")))) {
            log.info("Skip mail MQ publishing, notificationId={}, channel={}", notification.get("id"), notification.get("channel"));
            return;
        }
        if (!"PUBLISHED".equalsIgnoreCase(String.valueOf(notification.get("status")))) {
            log.info("Skip mail MQ publishing, notificationId={}, status={}", notification.get("id"), notification.get("status"));
            return;
        }

        List<String> recipients = resolveRecipients(notification, sourceBody);
        MailMessage message = new MailMessage(
                ((Number) notification.get("id")).longValue(),
                String.valueOf(notification.get("title")),
                Objects.toString(notification.get("content"), ""),
                recipients,
                Instant.now().toString());
        log.info("Publish mail notification, notificationId={}, recipients={}", notification.get("id"), recipients.size());
        mailMessagePublisher.publish(message);
    }

    private List<String> resolveRecipients(Map<String, Object> notification, Map<String, Object> sourceBody) {
        List<String> specifiedRecipients = parseRecipients(sourceBody.get("recipients"));
        if (!specifiedRecipients.isEmpty()) {
            return specifiedRecipients;
        }
        specifiedRecipients = parseRecipients(sourceBody.get("target_email"));
        if (!specifiedRecipients.isEmpty()) {
            return specifiedRecipients;
        }
        specifiedRecipients = parseRecipients(sourceBody.get("email"));
        if (!specifiedRecipients.isEmpty()) {
            return specifiedRecipients;
        }
        if ("ALL".equalsIgnoreCase(String.valueOf(notification.get("target_type")))) {
            List<Map<String, Object>> enabledUsers = userDirectoryClient.listEnabledUsers(internalServiceKey);
            if (enabledUsers == null || enabledUsers.isEmpty()) {
                return List.of();
            }
            return enabledUsers.stream()
                    .map(row -> row.get("email"))
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    private List<String> parseRecipients(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Iterable<?> iterable) {
            return toRecipientList(iterable);
        }
        return Arrays.stream(String.valueOf(value).split("[,;]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private List<String> toRecipientList(Iterable<?> iterable) {
        java.util.ArrayList<String> recipients = new java.util.ArrayList<>();
        for (Object item : iterable) {
            if (item != null && !String.valueOf(item).trim().isBlank()) {
                recipients.add(String.valueOf(item).trim());
            }
        }
        return recipients.stream().distinct().toList();
    }
}
