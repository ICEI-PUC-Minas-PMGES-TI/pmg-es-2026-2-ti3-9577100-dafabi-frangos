package com.dafabi.sales.application;

import com.dafabi.cash.application.CashRegisterService;
import com.dafabi.cash.domain.CashMovementDirection;
import com.dafabi.cash.domain.CashRegister;
import com.dafabi.cash.domain.CashRegisterStatus;
import com.dafabi.cash.repository.CashRegisterRepository;
import com.dafabi.inventory.domain.StockMovement;
import com.dafabi.inventory.domain.StockMovementType;
import com.dafabi.inventory.repository.StockMovementRepository;
import com.dafabi.products.domain.Product;
import com.dafabi.products.domain.ProductStatus;
import com.dafabi.products.domain.ProductStock;
import com.dafabi.products.repository.ProductStockRepository;
import com.dafabi.sales.domain.PaymentMethod;
import com.dafabi.sales.domain.Sale;
import com.dafabi.sales.domain.SaleItem;
import com.dafabi.sales.domain.SaleOrigin;
import com.dafabi.sales.domain.SaleStatus;
import com.dafabi.sales.dto.CreateSaleRequest;
import com.dafabi.sales.dto.SaleItemRequest;
import com.dafabi.sales.dto.SaleResponse;
import com.dafabi.sales.mapper.SaleMapper;
import com.dafabi.sales.repository.SaleRepository;
import com.dafabi.shared.exception.BusinessException;
import com.dafabi.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class SaleService {

    private static final String SALE_REFERENCE_TYPE = "SALE";

    private final SaleRepository saleRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashRegisterService cashRegisterService;
    private final ProductStockRepository productStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SaleMapper saleMapper;

    public SaleService(SaleRepository saleRepository,
                       CashRegisterRepository cashRegisterRepository,
                       CashRegisterService cashRegisterService,
                       ProductStockRepository productStockRepository,
                       StockMovementRepository stockMovementRepository,
                       SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashRegisterService = cashRegisterService;
        this.productStockRepository = productStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.saleMapper = saleMapper;
    }

    @Transactional
    public SaleResponse create(CreateSaleRequest request) {
        UUID storeId = request.storeId() != null ? request.storeId() : CashRegister.DEFAULT_STORE_ID;

        CashRegister register = cashRegisterRepository
                .findByStoreIdAndStatusForUpdate(storeId, CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BusinessException(
                        "CASH_CLOSED",
                        "Não existe caixa aberto para o operador. Abra o caixa antes de registrar vendas.",
                        HttpStatus.CONFLICT));

        UUID operatorId = resolveOperator(register, request.operatorId());

        Map<UUID, Integer> quantitiesByProduct = new TreeMap<>();
        for (SaleItemRequest item : request.items()) {
            quantitiesByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Sale sale = new Sale(
                UUID.randomUUID(),
                register.getId(),
                operatorId,
                saleRepository.findMaxSaleNumber() + 1,
                OffsetDateTime.now(),
                SaleOrigin.COUNTER,
                request.paymentMethod(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                SaleStatus.COMPLETED);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<UUID, Integer> entry : quantitiesByProduct.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();

            ProductStock stock = productStockRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
            Product product = stock.getProduct();

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException(
                        "PRODUCT_INACTIVE",
                        String.format("O produto \"%s\" está inativo e não pode ser vendido.", product.getName()));
            }

            int previousQuantity = stock.getQuantity();
            if (quantity > previousQuantity) {
                throw new BusinessException(
                        "INSUFFICIENT_STOCK",
                        String.format("Estoque insuficiente para \"%s\": solicitado %d, disponível %d.",
                                product.getName(), quantity, previousQuantity),
                        HttpStatus.CONFLICT);
            }

            BigDecimal unitPrice = product.getSalePrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            sale.addItem(new SaleItem(null, sale, product, BigDecimal.valueOf(quantity), unitPrice, itemSubtotal));
            subtotal = subtotal.add(itemSubtotal);

            int newQuantity = previousQuantity - quantity;
            stock.setQuantity(newQuantity);
            stockMovementRepository.save(new StockMovement(
                    product, null, StockMovementType.COUNTER_SALE,
                    previousQuantity, newQuantity, "Venda balcão",
                    "Venda nº " + sale.getSaleNumber(), register.getOperatorName(),
                    SALE_REFERENCE_TYPE, sale.getId()));
        }

        sale.setSubtotal(subtotal);
        sale.setTotal(subtotal);
        Sale saved = saleRepository.save(sale);

        if (saved.getPaymentMethod() == PaymentMethod.CASH) {
            cashRegisterService.recordMovement(
                    register.getId(),
                    CashMovementDirection.IN,
                    "SALE",
                    saved.getTotal(),
                    "Venda balcão",
                    "Venda nº " + saved.getSaleNumber(),
                    SALE_REFERENCE_TYPE,
                    saved.getId());
        }

        return saleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(UUID id) {
        return saleRepository.findWithItemsById(id)
                .map(saleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", id));
    }

    private UUID resolveOperator(CashRegister register, UUID requestedOperatorId) {
        UUID registerOperatorId = register.getOperatorId();
        if (requestedOperatorId != null && registerOperatorId != null && !requestedOperatorId.equals(registerOperatorId)) {
            throw new BusinessException(
                    "CASH_CLOSED",
                    "Não existe caixa aberto para o operador informado.",
                    HttpStatus.CONFLICT);
        }

        UUID operatorId = requestedOperatorId != null ? requestedOperatorId : registerOperatorId;
        if (operatorId == null) {
            throw new BusinessException("OPERATOR_REQUIRED", "O operador da venda é obrigatório.");
        }
        return operatorId;
    }
}
