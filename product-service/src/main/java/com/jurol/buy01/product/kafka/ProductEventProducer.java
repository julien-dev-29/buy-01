package com.jurol.buy01.product.kafka;

import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ProductEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${kafka.topics.product-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendProductCreatedEvent(ProductCreatedEvent event) {
        log.info("Sending ProductCreated event for product: {}", event.getProductId());
        kafkaTemplate.send(topic, event.getProductId(), event);
    }

    public void sendProductDeletedEvent(ProductDeletedEvent event) {
        log.info("Sending ProductDeleted event for product: {}", event.getProductId());
        kafkaTemplate.send(topic, event.getProductId(), event);
    }
}

