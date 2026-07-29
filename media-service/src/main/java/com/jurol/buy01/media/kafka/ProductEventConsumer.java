package com.jurol.buy01.media.kafka;

import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import com.jurol.buy01.media.service.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);
    private final MediaService mediaService;

    public ProductEventConsumer(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @KafkaListener(topics = "product-events", groupId = "media-service-group")
    public void handleProductEvent(Object event) {
        if (event instanceof ProductCreatedEvent created) {
            log.info("Received ProductCreated event for product: {}", created.getProductId());
        } else if (event instanceof ProductDeletedEvent deleted) {
            log.info("Received ProductDeleted event for product: {}", deleted.getProductId());
            mediaService.deleteMediaByProductId(deleted.getProductId());
        }
    }
}