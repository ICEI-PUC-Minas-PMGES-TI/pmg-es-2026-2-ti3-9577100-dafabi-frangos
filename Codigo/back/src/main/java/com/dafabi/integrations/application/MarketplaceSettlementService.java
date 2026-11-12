package com.dafabi.integrations.application;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.domain.MarketplaceSettlement;
import com.dafabi.integrations.dto.MarketplaceSettlementRequest;
import com.dafabi.integrations.dto.MarketplaceSettlementView;
import com.dafabi.integrations.repository.MarketplaceSettlementRepository;
import com.dafabi.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

@Service
public class MarketplaceSettlementService {
    private final MarketplaceSettlementRepository repository;
    private final ObjectMapper mapper;
    public MarketplaceSettlementService(MarketplaceSettlementRepository repository,ObjectMapper mapper){this.repository=repository;this.mapper=mapper;}

    @Transactional(readOnly=true)
    public List<MarketplaceSettlementView> list(IntegrationProvider provider,String storeId){
        requireStore(storeId);return repository.findByProviderAndExternalStoreIdOrderByPeriodStartDesc(provider,storeId)
                .stream().map(this::view).toList();
    }

    @Transactional
    public MarketplaceSettlementView save(IntegrationProvider provider,MarketplaceSettlementRequest request){
        requireStore(request.externalStoreId());
        if(request.periodStart().isAfter(request.periodEnd()))throw bad("INVALID_PERIOD","O início do período deve ser anterior ao fim.");
        if(!Set.of("PENDENTE","PAGO","RETIDO","NAO_NECESSARIO").contains(request.status()))throw bad("INVALID_STATUS","Situação do repasse inválida.");
        validateAmount(request.grossAmount(),false,"valor bruto");validateAmount(request.feesAmount(),false,"taxas");
        validateAmount(request.adjustmentsAmount(),true,"ajustes");validateAmount(request.retainedAmount(),false,"valor retido");
        validateAmount(request.amountReceived(),false,"valor recebido");
        if("PAGO".equals(request.status())&&(request.payoutDate()==null||request.amountReceived()==null))
            throw bad("PAYOUT_DETAILS_REQUIRED","Para marcar o repasse como pago, informe a data do crédito e o valor recebido.");
        String payload;
        try{payload=mapper.writeValueAsString(request);}catch(JsonProcessingException ex){throw new BusinessException("SETTLEMENT_SAVE_ERROR","Não foi possível registrar os dados do repasse.",HttpStatus.INTERNAL_SERVER_ERROR);}
        MarketplaceSettlement entity=repository.findByProviderAndExternalStoreIdAndExternalSettlementId(provider,
                request.externalStoreId(),request.externalSettlementId()).orElse(null);
        if(entity==null)entity=new MarketplaceSettlement(provider,request.externalStoreId(),request.externalSettlementId(),
                request.periodStart(),request.periodEnd(),request.grossAmount(),request.feesAmount(),request.adjustmentsAmount(),
                request.retainedAmount(),request.amountReceived(),request.payoutDate(),request.status(),request.sourceReference(),payload);
        else entity.update(request.periodStart(),request.periodEnd(),request.grossAmount(),request.feesAmount(),request.adjustmentsAmount(),
                request.retainedAmount(),request.amountReceived(),request.payoutDate(),request.status(),request.sourceReference(),payload);
        return view(repository.save(entity));
    }
    private MarketplaceSettlementView view(MarketplaceSettlement s){return new MarketplaceSettlementView(s.getId(),s.getProvider(),s.getExternalStoreId(),s.getExternalSettlementId(),s.getPeriodStart(),s.getPeriodEnd(),s.getGrossAmount(),s.getFeesAmount(),s.getAdjustmentsAmount(),s.getRetainedAmount(),s.getAmountReceived(),s.getPayoutDate(),s.getStatus(),s.getSourceReference(),s.getUpdatedAt());}
    private void validateAmount(BigDecimal amount,boolean negativeAllowed,String field){
        if(amount==null)return;
        if((!negativeAllowed&&amount.signum()<0)||amount.scale()>2||amount.precision()>14)
            throw bad("INVALID_SETTLEMENT_AMOUNT","Confira o campo "+field+": use no máximo duas casas decimais e respeite o sinal permitido.");
    }
    private void requireStore(String store){if(store==null||store.isBlank())throw bad("STORE_REQUIRED","Informe o identificador da loja no portal.");}
    private BusinessException bad(String code,String message){return new BusinessException(code,message,HttpStatus.BAD_REQUEST);}
}
