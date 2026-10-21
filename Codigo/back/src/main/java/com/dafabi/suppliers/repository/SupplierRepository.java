package com.dafabi.suppliers.repository;

import com.dafabi.suppliers.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID>, JpaSpecificationExecutor<Supplier> {

    boolean existsByStoreIdAndEmailIgnoreCase(UUID storeId, String email);

    boolean existsByStoreIdAndEmailIgnoreCaseAndIdNot(UUID storeId, String email, UUID id);

    List<Supplier> findByStoreIdOrderByNameAsc(UUID storeId);

    List<Supplier> findByStoreIdAndActiveOrderByNameAsc(UUID storeId, boolean active);
}
