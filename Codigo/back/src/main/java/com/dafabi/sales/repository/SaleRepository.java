package com.dafabi.sales.repository;

import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @Query("select coalesce(max(sale.saleNumber), 0) from Sale sale")
    Long findMaxSaleNumber();

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Sale> findWithItemsById(UUID id);

    @Query("""
        select s from Sale s
        where (:query is null or str(s.saleNumber) like concat('%', :query, '%'))
          and (:startDate is null or s.dateTime >= :startDate)
          and (:endDate is null or s.dateTime <= :endDate)
          and (:origin is null or s.origin = :origin)
          and (:status is null or s.status = :status)
          and (:paymentMethod is null or s.paymentMethod = :paymentMethod)
          and (:operatorId is null or s.operatorId = :operatorId)
        order by s.dateTime desc
        """)
    Page<Sale> findAllWithFilters(
            @Param("query") String query,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("origin") SaleOrigin origin,
            @Param("status") SaleStatus status,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("operatorId") UUID operatorId,
            Pageable pageable);
}
