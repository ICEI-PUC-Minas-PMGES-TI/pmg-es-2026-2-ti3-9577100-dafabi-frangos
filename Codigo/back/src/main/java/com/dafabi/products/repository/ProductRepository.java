package com.dafabi.products.repository;

import com.dafabi.products.domain.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends
        JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, UUID id);

    Optional<Product> findByBarcode(String barcode);

    @EntityGraph(attributePaths = { "category", "stock" })
    Optional<Product> findWithCategoryAndStockById(UUID id);
}