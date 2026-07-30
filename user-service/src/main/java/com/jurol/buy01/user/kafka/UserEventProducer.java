package com.jurol.buy01.user.kafka;

import com.jurol.buy01.common.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                             @Value("${kafka.topics.user-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Sending UserRegistered event for user: {}", event.getUserId());
        kafkaTemplate.send(topic, event.getUserId(), event);
    }
}

