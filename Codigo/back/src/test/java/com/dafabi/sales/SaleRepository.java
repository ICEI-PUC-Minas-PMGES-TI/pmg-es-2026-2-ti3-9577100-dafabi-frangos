package com.dafabi.sales.repository;

import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findWithItemsById(@Param("id") UUID id);

    @Query("SELECT COALESCE(MAX(s.saleNumber), 0) FROM Sale s")
    Long findMaxSaleNumber();

    @Query("""
        SELECT s FROM Sale s
        WHERE (:startDate IS NULL OR s.dateTime >= :startDate)
          AND (:endDate IS NULL OR s.dateTime <= :endDate)
          AND (:status IS NULL OR s.status = :status)
          AND (:paymentMethod IS NULL OR s.paymentMethod = :paymentMethod)
          AND (:operatorId IS NULL OR s.operatorId = :operatorId)
        ORDER BY s.dateTime DESC
    """)
    Page<Sale> findAllWithFilters(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("status") SaleStatus status,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("operatorId") UUID operatorId,
            Pageable pageable
    );
}
