package com.dafabi.inventory.repository;

import com.dafabi.inventory.domain.StockMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    @EntityGraph(attributePaths = "product")
    List<StockMovement> findByProduct_IdInOrderByOccurredAtDesc(Collection<UUID> productIds);

    @EntityGraph(attributePaths = "product")
    List<StockMovement> findByProduct_IdOrderByOccurredAtDesc(UUID productId);

    boolean existsByProduct_IdAndReferenceTypeAndOccurredAtAfter(
            UUID productId, String referenceType, OffsetDateTime occurredAt);
}
