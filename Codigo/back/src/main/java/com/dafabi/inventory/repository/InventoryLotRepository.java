package com.dafabi.inventory.repository;

import com.dafabi.inventory.domain.InventoryLot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryLotRepository extends JpaRepository<InventoryLot, UUID> {

    boolean existsByProduct_IdAndBatchIgnoreCase(UUID productId, String batch);

    @EntityGraph(attributePaths = "product")
    List<InventoryLot> findAllByOrderByExpiryAsc();
}
