package com.dafabi.integrations;

import com.dafabi.integrations.application.*;
import com.dafabi.integrations.domain.*;
import com.dafabi.integrations.dto.ImportSummary;
import com.dafabi.integrations.repository.*;
import com.dafabi.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImportOmittedItemsTest {
    private final ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
    private final IntegrationImportBatchRepository batches=mock(IntegrationImportBatchRepository.class);
    private final IntegrationImportRowRepository rows=mock(IntegrationImportRowRepository.class);
    private final MarketplaceItemSnapshotRepository items=mock(MarketplaceItemSnapshotRepository.class);
    private final IntegrationImportService service=new IntegrationImportService(mock(MarketplaceReportParser.class),batches,rows,
            mock(MarketplaceOrderRepository.class),items,mapper,10_000_000);
    private final UUID id=UUID.randomUUID();
    private final LocalDate date=LocalDate.of(2026,10,20);
    private final MarketplaceItemSnapshot absent=new MarketplaceItemSnapshot(UUID.randomUUID(),IntegrationProvider.IFOOD,
            "loja","Loja",date,"Item ausente","item ausente",BigDecimal.TEN,BigDecimal.valueOf(2),UUID.randomUUID(),"{}",OffsetDateTime.now());

    private void setup(LocalDate end,int invalidCount) throws Exception {
        var zero=BigDecimal.ZERO;
        var summary=new ImportSummary(1,0,0,0,zero,zero,zero,zero,zero,zero,zero,zero,zero,0,invalidCount);
        var batch=new IntegrationImportBatch(id,IntegrationProvider.IFOOD,IntegrationReportType.ITEMS,
                "relatorio.xlsx","hash",date,end,ImportBatchStatus.READY,1,1,invalidCount,0,
                mapper.writeValueAsString(summary),OffsetDateTime.now());
        var parsed=new ParsedItemRow("loja","Loja",date,"Item presente","item presente",BigDecimal.TEN,BigDecimal.ONE,Map.of());
        var row=new IntegrationImportRow(UUID.randomUUID(),id,1,"loja|item",true,false,false,"[]",mapper.writeValueAsString(parsed));
        when(batches.findById(id)).thenReturn(Optional.of(batch));
        when(batches.findByIdForUpdate(id)).thenReturn(Optional.of(batch));
        when(batches.save(any())).thenAnswer(call->call.getArgument(0));
        when(rows.findByBatchIdOrderByRowNumber(id)).thenReturn(List.of(row));
        when(items.findByProviderAndExternalStoreIdAndBusinessDateBetweenOrderByBusinessDateAscExternalItemNameAsc(
                IntegrationProvider.IFOOD,"loja",date,date)).thenReturn(List.of(absent));
        when(items.findRangeForUpdate(IntegrationProvider.IFOOD,"loja",date,date)).thenReturn(List.of(absent));
    }

    @Test
    void omittedItemIsOnlyZeroedWhenUserConfirmsTheCompleteReport() throws Exception {
        setup(date,0);
        assertThat(service.get(id).omittedItems()).singleElement().satisfies(item->assertThat(item.snapshotId()).isEqualTo(absent.getId()));
        service.confirm(id,List.of(absent.getId()));
        assertThat(absent.getSoldQuantity()).isZero();
        assertThat(absent.getItemRevenue()).isZero();
        verify(items).save(absent);
    }

    @Test
    void ordinaryImportDoesNotAssumeAnAbsentItemWasCancelled() throws Exception {
        setup(date,0);
        service.confirm(id,List.of());
        assertThat(absent.getSoldQuantity()).isEqualByComparingTo("2");
        verify(items,never()).save(absent);
    }

    @Test
    void refusesAConfirmationBasedOnAnOutdatedPreview() throws Exception {
        setup(date,0);
        assertThatThrownBy(()->service.confirm(id,List.of(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("prévia");
        assertThat(absent.getSoldQuantity()).isEqualByComparingTo("2");
    }

    @Test
    void multiDayReportCannotBeTreatedAsACompleteDailySnapshot() throws Exception {
        setup(date.plusDays(1),0);
        assertThat(service.get(id).omittedItems()).isEmpty();
        assertThatThrownBy(()->service.confirm(id,List.of(absent.getId()))).isInstanceOf(BusinessException.class);
        assertThat(absent.getSoldQuantity()).isEqualByComparingTo("2");
    }
}
