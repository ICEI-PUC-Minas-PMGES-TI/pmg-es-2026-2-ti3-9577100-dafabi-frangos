package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_order", uniqueConstraints = @UniqueConstraint(name="uk_marketplace_order_external", columnNames={"provider","external_store_id","external_order_id"}))
public class MarketplaceOrder {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private IntegrationProvider provider;
    @Column(name="external_store_id",nullable=false,length=120) private String externalStoreId;
    @Column(name="store_name") private String storeName;
    @Column(name="external_order_id",nullable=false,length=160) private String externalOrderId;
    @Column(name="order_at",nullable=false) private OffsetDateTime orderAt;
    @Column(name="raw_status",nullable=false,length=100) private String rawStatus;
    @Column(name="normalized_status",nullable=false,length=32) private String normalizedStatus;
    @Column(name="shift_name",length=80) private String shiftName;
    @Column(name="items_value",precision=14,scale=2) private BigDecimal itemsValue;
    @Column(name="customer_paid",precision=14,scale=2) private BigDecimal customerPaid;
    @Column(name="customer_delivery_fee",precision=14,scale=2) private BigDecimal customerDeliveryFee;
    @Column(name="ifood_promotion",precision=14,scale=2) private BigDecimal ifoodPromotion;
    @Column(name="store_promotion",precision=14,scale=2) private BigDecimal storePromotion;
    @Column(name="network_promotion",precision=14,scale=2) private BigDecimal networkPromotion;
    @Column(name="service_fee",precision=14,scale=2) private BigDecimal serviceFee;
    @Column(name="fees_and_commissions",precision=14,scale=2) private BigDecimal feesAndCommissions;
    @Column(name="net_reported",precision=14,scale=2) private BigDecimal netReported;
    @Column(name="payment_method_raw",length=160) private String paymentMethodRaw;
    @Column(name="delivery_type",length=100) private String deliveryType;
    @Column(name="logistics_product",length=160) private String logisticsProduct;
    @Column(name="sales_channel",length=100) private String salesChannel;
    @Column(name="marketing_cost",precision=14,scale=2) private BigDecimal marketingCost;
    @Column(name="logistics_cost",precision=14,scale=2) private BigDecimal logisticsCost;
    @Column(name="refund_amount",precision=14,scale=2) private BigDecimal refundAmount;
    @Column(name="free_delivery_cost",precision=14,scale=2) private BigDecimal freeDeliveryCost;
    @Column(name="tip_amount",precision=14,scale=2) private BigDecimal tipAmount;
    @Column(name="original_item_count",precision=14,scale=3) private BigDecimal originalItemCount;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @Column(name="cancelled_at") private OffsetDateTime cancelledAt;
    @Column(name="cancellation_responsible",length=180) private String cancellationResponsible;
    @Column(name="cancellation_reason",length=500) private String cancellationReason;
    @Column(name="source_batch_id",nullable=false) private UUID sourceBatchId;
    @Column(name="source_payload_json",nullable=false,columnDefinition="TEXT") private String sourcePayloadJson;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;

    protected MarketplaceOrder() {}
    public MarketplaceOrder(UUID id, IntegrationProvider provider, String externalStoreId, String storeName,
            String externalOrderId, OffsetDateTime orderAt, String rawStatus, String normalizedStatus,
            String shiftName, BigDecimal itemsValue, BigDecimal customerPaid, BigDecimal customerDeliveryFee,
            BigDecimal ifoodPromotion, BigDecimal storePromotion, BigDecimal networkPromotion,
            BigDecimal serviceFee, BigDecimal feesAndCommissions, BigDecimal netReported,
            String paymentMethodRaw, String deliveryType, String logisticsProduct, String salesChannel,
            BigDecimal marketingCost,BigDecimal logisticsCost,BigDecimal refundAmount,BigDecimal freeDeliveryCost,
            BigDecimal tipAmount,BigDecimal originalItemCount,OffsetDateTime completedAt,OffsetDateTime cancelledAt,
            String cancellationResponsible,String cancellationReason,
            UUID sourceBatchId, String sourcePayloadJson, OffsetDateTime updatedAt) {
        this.id=id; this.provider=provider; this.externalStoreId=externalStoreId; this.storeName=storeName;
        this.externalOrderId=externalOrderId; this.orderAt=orderAt; this.rawStatus=rawStatus;
        this.normalizedStatus=normalizedStatus; this.shiftName=shiftName; this.itemsValue=itemsValue;
        this.customerPaid=customerPaid; this.customerDeliveryFee=customerDeliveryFee; this.ifoodPromotion=ifoodPromotion;
        this.storePromotion=storePromotion; this.networkPromotion=networkPromotion; this.serviceFee=serviceFee;
        this.feesAndCommissions=feesAndCommissions; this.netReported=netReported; this.paymentMethodRaw=paymentMethodRaw;
        this.deliveryType=deliveryType; this.logisticsProduct=logisticsProduct; this.salesChannel=salesChannel;
        this.marketingCost=marketingCost;this.logisticsCost=logisticsCost;this.refundAmount=refundAmount;
        this.freeDeliveryCost=freeDeliveryCost;this.tipAmount=tipAmount;this.originalItemCount=originalItemCount;
        this.completedAt=completedAt;this.cancelledAt=cancelledAt;this.cancellationResponsible=cancellationResponsible;
        this.cancellationReason=cancellationReason;
        this.sourceBatchId=sourceBatchId; this.sourcePayloadJson=sourcePayloadJson; this.updatedAt=updatedAt;
    }
    public void replaceFrom(MarketplaceOrder o){
        storeName=o.storeName; orderAt=o.orderAt; rawStatus=o.rawStatus; normalizedStatus=o.normalizedStatus;
        shiftName=o.shiftName; itemsValue=o.itemsValue; customerPaid=o.customerPaid; customerDeliveryFee=o.customerDeliveryFee;
        ifoodPromotion=o.ifoodPromotion; storePromotion=o.storePromotion; networkPromotion=o.networkPromotion;
        serviceFee=o.serviceFee; feesAndCommissions=o.feesAndCommissions; netReported=o.netReported;
        paymentMethodRaw=o.paymentMethodRaw; deliveryType=o.deliveryType; logisticsProduct=o.logisticsProduct;
        salesChannel=o.salesChannel;marketingCost=o.marketingCost;logisticsCost=o.logisticsCost;refundAmount=o.refundAmount;
        freeDeliveryCost=o.freeDeliveryCost;tipAmount=o.tipAmount;originalItemCount=o.originalItemCount;
        completedAt=o.completedAt;cancelledAt=o.cancelledAt;cancellationResponsible=o.cancellationResponsible;
        cancellationReason=o.cancellationReason;sourceBatchId=o.sourceBatchId;sourcePayloadJson=o.sourcePayloadJson;updatedAt=o.updatedAt;
    }
    public UUID getId(){return id;} public IntegrationProvider getProvider(){return provider;}
    public String getExternalStoreId(){return externalStoreId;} public String getExternalOrderId(){return externalOrderId;}
    public OffsetDateTime getOrderAt(){return orderAt;} public String getRawStatus(){return rawStatus;}
    public String getNormalizedStatus(){return normalizedStatus;} public BigDecimal getItemsValue(){return itemsValue;}
    public BigDecimal getCustomerPaid(){return customerPaid;} public BigDecimal getNetReported(){return netReported;}
    public BigDecimal getFeesAndCommissions(){return feesAndCommissions;} public BigDecimal getServiceFee(){return serviceFee;}
    public BigDecimal getStorePromotion(){return storePromotion;}
    public BigDecimal getMarketingCost(){return marketingCost;} public BigDecimal getLogisticsCost(){return logisticsCost;}
    public BigDecimal getRefundAmount(){return refundAmount;} public BigDecimal getFreeDeliveryCost(){return freeDeliveryCost;}
    public BigDecimal getTipAmount(){return tipAmount;} public BigDecimal getOriginalItemCount(){return originalItemCount;}
    public OffsetDateTime getCompletedAt(){return completedAt;} public OffsetDateTime getCancelledAt(){return cancelledAt;}
    public String getCancellationResponsible(){return cancellationResponsible;} public String getCancellationReason(){return cancellationReason;}
    public String getPaymentMethodRaw(){return paymentMethodRaw;}
}
