package com.dafabi.products.repository;

import com.dafabi.products.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByActiveTrueOrderByNameAsc();
}
