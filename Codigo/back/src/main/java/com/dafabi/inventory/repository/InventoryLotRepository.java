package com.dafabi.inventory.repository;

import com.dafabi.inventory.domain.InventoryLot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface InventoryLotRepository extends JpaRepository<InventoryLot, UUID> {

    boolean existsByProduct_IdAndBatchIgnoreCase(UUID productId, String batch);

    @EntityGraph(attributePaths = "product")
    List<InventoryLot> findAllByOrderByExpiryAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lot from InventoryLot lot where lot.product.id = :productId and lot.quantity > 0 " +
            "order by lot.expiry asc, lot.receivedAt asc, lot.createdAt asc")
    List<InventoryLot> findAvailableByProductForUpdate(@Param("productId") UUID productId);
}
