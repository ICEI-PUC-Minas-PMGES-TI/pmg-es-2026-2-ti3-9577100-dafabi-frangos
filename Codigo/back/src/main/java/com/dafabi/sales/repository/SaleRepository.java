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
        where (:hasQuery = false
               or cast(s.saleNumber as string) like concat('%', cast(:query as string), '%'))
          and (:hasStartDate = false or s.dateTime >= :startDate)
          and (:hasEndDate = false or s.dateTime <= :endDate)
          and (:hasOrigin = false or s.origin = :origin)
          and (:hasStatus = false or s.status = :status)
          and (:hasPaymentMethod = false or s.paymentMethod = :paymentMethod)
          and (:hasOperatorId = false or s.operatorId = :operatorId)
        order by s.dateTime desc
        """)
    Page<Sale> findAllWithFilters(
            @Param("query") String query,
            @Param("hasQuery") boolean hasQuery,
            @Param("startDate") OffsetDateTime startDate,
            @Param("hasStartDate") boolean hasStartDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("hasEndDate") boolean hasEndDate,
            @Param("origin") SaleOrigin origin,
            @Param("hasOrigin") boolean hasOrigin,
            @Param("status") SaleStatus status,
            @Param("hasStatus") boolean hasStatus,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("hasPaymentMethod") boolean hasPaymentMethod,
            @Param("operatorId") UUID operatorId,
            @Param("hasOperatorId") boolean hasOperatorId,
            Pageable pageable);
}
