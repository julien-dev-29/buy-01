package com.jurol.buy01.product.service;

import com.jurol.buy01.common.dto.ProductDTO;
import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import com.jurol.buy01.product.kafka.ProductEventProducer;
import com.jurol.buy01.product.model.Product;
import com.jurol.buy01.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventProducer eventProducer;

    public ProductService(ProductRepository productRepository, ProductEventProducer eventProducer) {
        this.productRepository = productRepository;
        this.eventProducer = eventProducer;
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDTO(product);
    }

    public ProductDTO createProduct(ProductDTO dto, String sellerId) {
        Product product = new Product(dto.getName(), dto.getDescription(), dto.getPrice(), sellerId);
        Product saved = productRepository.save(product);

        eventProducer.sendProductCreatedEvent(new ProductCreatedEvent(saved.getId(), sellerId));

        return toDTO(saved);
    }

    public ProductDTO updateProduct(String id, ProductDTO dto, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized to update this product");
        }

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());

        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    public void deleteProduct(String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized to delete this product");
        }

        productRepository.deleteById(id);
        eventProducer.sendProductDeletedEvent(new ProductDeletedEvent(id, sellerId));
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSellerId(),
                product.getMediaIds(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}