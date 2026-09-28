package com.dafabi.sales.repository;

import com.dafabi.sales.domain.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @Query("select coalesce(max(sale.saleNumber), 0) from Sale sale")
    Long findMaxSaleNumber();

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Sale> findWithItemsById(UUID id);
}
