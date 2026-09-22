package com.dafabi.products.application;

import com.dafabi.products.dto.CategoryResponse;
import com.dafabi.products.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findActive() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }
}
