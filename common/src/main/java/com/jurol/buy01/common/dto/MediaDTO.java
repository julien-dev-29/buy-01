package com.jurol.buy01.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaDTO {
    private String id;
    private String filename;
    private String originalName;
    private String contentType;
    private Long size;
    private String productId;
    private String sellerId;
    private Instant createdAt;

    public MediaDTO() {}

    public MediaDTO(String id, String filename, String originalName, String contentType, Long size, String productId, String sellerId, Instant createdAt) {
        this.id = id;
        this.filename = filename;
        this.originalName = originalName;
        this.contentType = contentType;
        this.size = size;
        this.productId = productId;
        this.sellerId = sellerId;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

