package com.dafabi.sales.repository;

import com.dafabi.sales.domain.SaleRefund;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRefundRepository extends JpaRepository<SaleRefund, UUID> {

    @EntityGraph(attributePaths = "sale")
    Optional<SaleRefund> findBySale_Id(UUID saleId);

    @EntityGraph(attributePaths = "sale")
    Optional<SaleRefund> findByIdempotencyKey(String idempotencyKey);

    boolean existsBySale_Id(UUID saleId);
}
