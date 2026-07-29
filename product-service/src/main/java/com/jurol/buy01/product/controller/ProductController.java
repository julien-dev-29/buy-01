package com.jurol.buy01.product.controller;

import com.jurol.buy01.common.dto.ProductDTO;
import com.jurol.buy01.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(Authentication authentication, @Valid @RequestBody ProductDTO dto) {
        String sellerId = (String) authentication.getPrincipal();
        ProductDTO created = productService.createProduct(dto, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(Authentication authentication, @PathVariable String id, @RequestBody ProductDTO dto) {
        String sellerId = (String) authentication.getPrincipal();
        ProductDTO updated = productService.updateProduct(id, dto, sellerId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(Authentication authentication, @PathVariable String id) {
        String sellerId = (String) authentication.getPrincipal();
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}