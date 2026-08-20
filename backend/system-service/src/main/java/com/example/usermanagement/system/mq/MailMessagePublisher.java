package com.example.usermanagement.system.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MailMessagePublisher {
    private static final Logger log = LoggerFactory.getLogger(MailMessagePublisher.class);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String namesrvAddr;
    private final String topic;
    private final String tag;
    private final DefaultMQProducer producer;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public MailMessagePublisher(
            ObjectMapper objectMapper,
            @Value("${app.mq.enabled:false}") boolean enabled,
            @Value("${app.mq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
            @Value("${app.mq.mail.topic:risk-mail-topic}") String topic,
            @Value("${app.mq.mail.tag:mail-send}") String tag,
            @Value("${app.mq.mail.producer-group:risk-mail-producer-group}") String producerGroup) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
        this.tag = tag;
        this.producer = new DefaultMQProducer(producerGroup);
        this.producer.setNamesrvAddr(namesrvAddr);
    }

    public void publish(MailMessage message) {
        if (!enabled) {
            log.info("RocketMQ mail publishing is disabled, skip notification: {}", message.notificationId());
            return;
        }
        try {
            startIfNeeded();
            byte[] body = objectMapper.writeValueAsBytes(message);
            Message rocketMessage = new Message(topic, tag, String.valueOf(message.notificationId()), body);
            SendResult result = producer.send(rocketMessage);
            log.info("Published mail notification to RocketMQ, notificationId={}, topic={}, tag={}, msgId={}, status={}",
                    message.notificationId(), topic, tag, result.getMsgId(), result.getSendStatus());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Publish mail notification to RocketMQ interrupted: {}, namesrv={}", message.notificationId(), namesrvAddr, ex);
        } catch (JsonProcessingException | MQClientException | RemotingException | MQBrokerException ex) {
            log.error("Publish mail notification to RocketMQ failed: {}, namesrv={}", message.notificationId(), namesrvAddr, ex);
        }
    }

    private void startIfNeeded() throws MQClientException {
        if (started.compareAndSet(false, true)) {
            producer.start();
            log.info("RocketMQ mail producer started, namesrv={}, topic={}, tag={}", namesrvAddr, topic, tag);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (started.get()) {
            producer.shutdown();
        }
    }
}
