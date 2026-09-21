package com.dafabi.products.repository;

import com.dafabi.products.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByBarcode(String barcode);

    Optional<Product> findByBarcode(String barcode);
}
