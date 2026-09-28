package com.dafabi.sales.application;

import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.dto.SaleResponse;
import com.dafabi.sales.mapper.SaleMapper;
import com.dafabi.sales.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;

    public SaleService(SaleRepository saleRepository, SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.saleMapper = saleMapper;
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> findAll(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            SaleStatus status,
            PaymentMethod paymentMethod,
            UUID operatorId,
            Pageable pageable
    ) {
        Page<Sale> sales = saleRepository.findAllWithFilters(
                startDate,
                endDate,
                status,
                paymentMethod,
                operatorId,
                pageable
        );

        return sales.map(saleMapper::toResponse);
    }
}
