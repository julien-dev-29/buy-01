package com.jurol.buy01.product.kafka;

import com.jurol.buy01.common.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    @KafkaListener(topics = "user-events", groupId = "product-service-group")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegistered event for user: {} with role: {}", event.getUserId(), event.getRole());
    }
}

