package com.jurol.buy01.common.events;

import java.time.Instant;

public class ProductDeletedEvent {
    private String eventType = "PRODUCT_DELETED";
    private String productId;
    private String sellerId;
    private Instant timestamp;

    public ProductDeletedEvent() {}

    public ProductDeletedEvent(String productId, String sellerId) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.timestamp = Instant.now();
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}