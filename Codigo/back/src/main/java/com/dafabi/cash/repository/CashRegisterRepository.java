package com.dafabi.cash.repository;

import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, UUID> {

    Optional<CashRegister> findByStoreIdAndStatus(UUID storeId, CashRegisterStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select register from CashRegister register where register.storeId = :storeId and register.status = :status")
    Optional<CashRegister> findByStoreIdAndStatusForUpdate(@Param("storeId") UUID storeId,
                                                           @Param("status") CashRegisterStatus status);

    boolean existsByStoreIdAndStatus(UUID storeId, CashRegisterStatus status);

    Optional<CashRegister> findFirstByStoreIdOrderByOpenedAtDesc(UUID storeId);

    List<CashRegister> findByStoreIdOrderByOpenedAtDesc(UUID storeId);
}
