package com.dafabi.suppliers.mapper;

import com.dafabi.suppliers.domain.Supplier;
import com.dafabi.suppliers.dto.CreateSupplierRequest;
import com.dafabi.suppliers.dto.SupplierResponse;
import com.dafabi.suppliers.dto.UpdateSupplierRequest;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class SupplierMapper {

    public Supplier toEntity(CreateSupplierRequest request) {
        if (request == null) {
            return null;
        }

        UUID storeId = request.storeId() != null ? request.storeId() : Supplier.DEFAULT_STORE_ID;
        boolean active = request.active() == null || request.active();
        OffsetDateTime now = OffsetDateTime.now();

        return new Supplier(
                UUID.randomUUID(),
                storeId,
                request.name(),
                request.phone(),
                request.email(),
                request.notes(),
                active,
                now,
                now
        );
    }

    public void updateEntity(Supplier supplier, UpdateSupplierRequest request) {
        if (supplier == null || request == null) {
            return;
        }

        supplier.setName(request.name());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setNotes(request.notes());
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
    }

    public SupplierResponse toResponse(Supplier supplier) {
        if (supplier == null) {
            return null;
        }

        return new SupplierResponse(
                supplier.getId(),
                supplier.getStoreId(),
                supplier.getName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getNotes(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
