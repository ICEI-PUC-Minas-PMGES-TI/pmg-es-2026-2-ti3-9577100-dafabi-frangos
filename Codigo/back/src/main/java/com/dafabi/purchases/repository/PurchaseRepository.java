package com.dafabi.purchases.repository;

import com.dafabi.purchases.domain.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID>, JpaSpecificationExecutor<Purchase> {

    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    Optional<Purchase> findWithDetailsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    @Query("select p from Purchase p where p.id = :id")
    Optional<Purchase> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"supplier", "items"})
    List<Purchase> findAllByOrderByPurchaseDateDescCreatedAtDesc();

    @EntityGraph(attributePaths = {"supplier", "items"})
    List<Purchase> findBySupplier_IdOrderByPurchaseDateDescCreatedAtDesc(UUID supplierId);
}
