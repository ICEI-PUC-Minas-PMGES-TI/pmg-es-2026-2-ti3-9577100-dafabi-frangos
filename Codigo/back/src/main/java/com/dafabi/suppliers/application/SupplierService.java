package com.dafabi.suppliers.application;

import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import com.dafabi.suppliers.domain.Supplier;
import com.dafabi.suppliers.dto.CreateSupplierRequest;
import com.dafabi.suppliers.dto.SupplierResponse;
import com.dafabi.suppliers.dto.UpdateSupplierRequest;
import com.dafabi.suppliers.mapper.SupplierMapper;
import com.dafabi.suppliers.repository.SupplierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        UUID storeId = request.storeId() != null ? request.storeId() : Supplier.DEFAULT_STORE_ID;
        String normalizedEmail = request.email().trim().toLowerCase();

        if (supplierRepository.existsByStoreIdAndEmailIgnoreCase(storeId, normalizedEmail)) {
            throw new BusinessException(
                    "DUPLICATE_EMAIL",
                    "Já existe um fornecedor cadastrado com este e-mail.",
                    HttpStatus.CONFLICT
            );
        }

        Supplier supplier = supplierMapper.toEntity(request);
        Supplier saved = supplierRepository.save(supplier);
        return supplierMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll(String query, Boolean active, UUID storeId) {
        UUID resolvedStoreId = storeId != null ? storeId : Supplier.DEFAULT_STORE_ID;

        Specification<Supplier> specification = (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("storeId"), resolvedStoreId);

        if (active != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("active"), active));
        }

        if (query != null && !query.isBlank()) {
            String search = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), search),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), search),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), search)
                    ));
        }

        Sort sort = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
        return supplierRepository.findAll(specification, sort).stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));
        return supplierMapper.toResponse(supplier);
    }

    @Transactional
    public SupplierResponse update(UUID id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));

        String normalizedEmail = request.email().trim().toLowerCase();
        if (supplierRepository.existsByStoreIdAndEmailIgnoreCaseAndIdNot(supplier.getStoreId(), normalizedEmail, id)) {
            throw new BusinessException(
                    "DUPLICATE_EMAIL",
                    "Já existe um fornecedor cadastrado com este e-mail.",
                    HttpStatus.CONFLICT
            );
        }

        supplierMapper.updateEntity(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse updateStatus(UUID id, boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));

        supplier.setActive(active);
        Supplier saved = supplierRepository.save(supplier);
        return supplierMapper.toResponse(saved);
    }
}
