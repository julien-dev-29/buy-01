package com.jurol.buy01.product.service;

import com.jurol.buy01.common.dto.ProductDTO;
import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import com.jurol.buy01.product.kafka.ProductEventProducer;
import com.jurol.buy01.product.model.Product;
import com.jurol.buy01.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductEventProducer eventProducer;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = new Product("Test Product", "Description", new BigDecimal("29.99"), "seller123");
        product.setId("product123");

        productDTO = new ProductDTO("product123", "Test Product", "Description", new BigDecimal("29.99"), "seller123", null, null, null);
    }

    @Test
    void createProduct_shouldCreateAndPublishEvent() {
        when(productRepository.save(any(Product.class))).thenReturn(product);
        doNothing().when(eventProducer).sendProductCreatedEvent(any(ProductCreatedEvent.class));

        ProductDTO result = productService.createProduct(productDTO, "seller123");

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(eventProducer).sendProductCreatedEvent(any(ProductCreatedEvent.class));
    }

    @Test
    void updateProduct_shouldUpdateIfOwner() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO updateDto = new ProductDTO();
        updateDto.setName("Updated Product");

        ProductDTO result = productService.updateProduct("product123", updateDto, "seller123");

        assertEquals("Updated Product", result.getName());
    }

    @Test
    void updateProduct_shouldThrowIfNotOwner() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));

        ProductDTO updateDto = new ProductDTO();
        updateDto.setName("Hacked Product");

        assertThrows(RuntimeException.class, () ->
                productService.updateProduct("product123", updateDto, "other-seller"));
    }

    @Test
    void deleteProduct_shouldDeleteAndPublishEvent() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));
        doNothing().when(productRepository).deleteById("product123");
        doNothing().when(eventProducer).sendProductDeletedEvent(any(ProductDeletedEvent.class));

        productService.deleteProduct("product123", "seller123");

        verify(productRepository).deleteById("product123");
        verify(eventProducer).sendProductDeletedEvent(any(ProductDeletedEvent.class));
    }

    @Test
    void deleteProduct_shouldThrowIfNotOwner() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                productService.deleteProduct("product123", "other-seller"));
    }
}