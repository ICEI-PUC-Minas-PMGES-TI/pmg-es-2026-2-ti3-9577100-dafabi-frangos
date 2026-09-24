package com.dafabi.cash.repository;

import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, UUID> {

    Optional<CashRegister> findByStoreIdAndStatus(UUID storeId, CashRegisterStatus status);

    boolean existsByStoreIdAndStatus(UUID storeId, CashRegisterStatus status);

    Optional<CashRegister> findFirstByStoreIdOrderByOpenedAtDesc(UUID storeId);

    List<CashRegister> findByStoreIdOrderByOpenedAtDesc(UUID storeId);
}
