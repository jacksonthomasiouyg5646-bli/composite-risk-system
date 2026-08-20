package com.example.usermanagement.system.mq;

import com.example.usermanagement.system.mail.MailSenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
public class MailMessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(MailMessageConsumer.class);

    private final MailSenderService mailSenderService;
    private final ObjectMapper objectMapper;
    private final boolean consumeEnabled;
    private final String namesrvAddr;
    private final String topic;
    private final String tag;
    private final DefaultMQPushConsumer consumer;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public MailMessageConsumer(
            MailSenderService mailSenderService,
            ObjectMapper objectMapper,
            @Value("${app.mq.mail.consume-enabled:true}") boolean consumeEnabled,
            @Value("${app.mq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
            @Value("${app.mq.mail.topic:risk-mail-topic}") String topic,
            @Value("${app.mq.mail.tag:mail-send}") String tag,
            @Value("${app.mq.mail.consumer-group:risk-mail-consumer-group}") String consumerGroup) {
        this.mailSenderService = mailSenderService;
        this.objectMapper = objectMapper;
        this.consumeEnabled = consumeEnabled;
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
        this.tag = tag;
        this.consumer = new DefaultMQPushConsumer(consumerGroup);
        this.consumer.setNamesrvAddr(namesrvAddr);
    }

    @PostConstruct
    public void start() throws MQClientException {
        if (!consumeEnabled) {
            log.info("RocketMQ mail consumer is disabled, topic={}", topic);
            return;
        }
        try {
            consumer.subscribe(topic, tag);
            consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
                for (org.apache.rocketmq.common.message.MessageExt messageExt : messages) {
                    try {
                        MailMessage message = objectMapper.readValue(messageExt.getBody(), MailMessage.class);
                        log.info("Consume RocketMQ mail notification: {}, msgId={}", message.notificationId(), messageExt.getMsgId());
                        mailSenderService.send(message);
                    } catch (Exception ex) {
                        log.error("Consume RocketMQ mail notification failed, msgId={}", messageExt.getMsgId(), ex);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            consumer.start();
            started.set(true);
            log.info("RocketMQ mail consumer started, namesrv={}, topic={}, tag={}", namesrvAddr, topic, tag);
        } catch (Exception ex) {
            log.error("RocketMQ mail consumer failed to start, namesrv={}, topic={}, tag={}. System service will continue without mail consuming.",
                    namesrvAddr, topic, tag, ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (started.get()) {
            consumer.shutdown();
        }
    }
}
