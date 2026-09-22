package com.dafabi.products.controller;

import com.dafabi.products.application.ProductService;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.dto.CreateProductRequest;
import com.dafabi.products.dto.ProductListResponse;
import com.dafabi.products.dto.ProductResponse;
import com.dafabi.products.dto.UpdateProductRequest;
import com.dafabi.products.dto.UpdateProductStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductListResponse>> findAll(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                productService.findAll(
                        query,
                        categoryId,
                        status,
                        includeInactive,
                        page,
                        size));
    }

    @GetMapping("/by-barcode/{barcode}")
    public ResponseEntity<ProductResponse> findByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.findByBarcode(barcode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(
            @PathVariable UUID id) {
        ProductResponse response = productService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(
                productService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        return ResponseEntity.ok(
                productService.updateStatus(id, request.status()));
    }
}
