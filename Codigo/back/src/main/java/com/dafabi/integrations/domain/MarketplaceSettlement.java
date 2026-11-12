package com.dafabi.integrations.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="marketplace_settlement",uniqueConstraints=@UniqueConstraint(name="uk_marketplace_settlement_external",
        columnNames={"provider","external_store_id","external_settlement_id"}))
public class MarketplaceSettlement {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private IntegrationProvider provider;
    @Column(name="external_store_id",nullable=false,length=120) private String externalStoreId;
    @Column(name="external_settlement_id",nullable=false,length=160) private String externalSettlementId;
    @Column(name="period_start",nullable=false) private LocalDate periodStart;
    @Column(name="period_end",nullable=false) private LocalDate periodEnd;
    @Column(name="gross_amount",precision=14,scale=2) private BigDecimal grossAmount;
    @Column(name="fees_amount",precision=14,scale=2) private BigDecimal feesAmount;
    @Column(name="adjustments_amount",precision=14,scale=2) private BigDecimal adjustmentsAmount;
    @Column(name="retained_amount",precision=14,scale=2) private BigDecimal retainedAmount;
    @Column(name="amount_received",precision=14,scale=2) private BigDecimal amountReceived;
    @Column(name="payout_date") private LocalDate payoutDate;
    @Column(nullable=false,length=40) private String status;
    @Column(name="source_batch_id") private UUID sourceBatchId;
    @Column(name="source_reference",columnDefinition="TEXT") private String sourceReference;
    @Column(name="source_payload_json",nullable=false,columnDefinition="TEXT") private String sourcePayloadJson;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    protected MarketplaceSettlement() {}
    public MarketplaceSettlement(IntegrationProvider provider,String storeId,String settlementId,LocalDate start,
            LocalDate end,BigDecimal gross,BigDecimal fees,BigDecimal adjustments,BigDecimal retained,
            BigDecimal received,LocalDate payoutDate,String status,String sourceReference,String sourcePayloadJson){
        this.id=UUID.randomUUID();this.provider=provider;this.externalStoreId=storeId;this.externalSettlementId=settlementId;
        update(start,end,gross,fees,adjustments,retained,received,payoutDate,status,sourceReference,sourcePayloadJson);
    }
    public void update(LocalDate start,LocalDate end,BigDecimal gross,BigDecimal fees,BigDecimal adjustments,
            BigDecimal retained,BigDecimal received,LocalDate payoutDate,String status,String sourceReference,String payload){
        this.periodStart=start;this.periodEnd=end;this.grossAmount=gross;this.feesAmount=fees;this.adjustmentsAmount=adjustments;
        this.retainedAmount=retained;this.amountReceived=received;this.payoutDate=payoutDate;this.status=status;
        this.sourceReference=sourceReference;this.sourcePayloadJson=payload;this.updatedAt=OffsetDateTime.now();
    }
    public UUID getId(){return id;}public IntegrationProvider getProvider(){return provider;}public String getExternalStoreId(){return externalStoreId;}
    public String getExternalSettlementId(){return externalSettlementId;}public LocalDate getPeriodStart(){return periodStart;}public LocalDate getPeriodEnd(){return periodEnd;}
    public BigDecimal getGrossAmount(){return grossAmount;}public BigDecimal getFeesAmount(){return feesAmount;}public BigDecimal getAdjustmentsAmount(){return adjustmentsAmount;}
    public BigDecimal getRetainedAmount(){return retainedAmount;}public BigDecimal getAmountReceived(){return amountReceived;}public LocalDate getPayoutDate(){return payoutDate;}
    public String getStatus(){return status;}public String getSourceReference(){return sourceReference;}public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
