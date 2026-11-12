package com.dafabi.cash.repository;

import com.dafabi.cash.domain.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {

    List<CashMovement> findByCashRegister_IdOrderByOccurredAtDesc(UUID cashRegisterId);

    List<CashMovement> findByCashRegister_IdOrderByOccurredAtAsc(UUID cashRegisterId);

    List<CashMovement> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

    List<CashMovement> findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            OffsetDateTime start, OffsetDateTime endExclusive);
}
