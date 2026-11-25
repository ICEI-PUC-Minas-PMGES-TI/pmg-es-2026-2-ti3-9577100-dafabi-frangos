package com.dafabi.integrations;

import com.dafabi.integrations.application.MarketplaceReportParser;
import com.dafabi.integrations.domain.IntegrationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarketplaceActualReportsTest {
    @Test
    @EnabledIfEnvironmentVariable(named="DAFABI_REPORT_DIR",matches=".+")
    void parsesActualMarketplaceReportsWithoutCommittingCustomerFiles() throws Exception {
        Path directory=Path.of(System.getenv("DAFABI_REPORT_DIR"));
        var parser=new MarketplaceReportParser();
        int files=0;
        try(var entries=Files.list(directory)) {
            for(Path path:entries.toList()) {
                String name=path.getFileName().toString();
                IntegrationProvider provider;
                if(name.startsWith("_Dados do pedido") && name.endsWith(".zip")) provider=IntegrationProvider.FOOD_99;
                else if(name.startsWith("_Dados do item") && name.endsWith(".zip")) provider=IntegrationProvider.FOOD_99;
                else if(name.startsWith("relatorio-pedidos_") && name.endsWith(".xlsx")) provider=IntegrationProvider.IFOOD;
                else continue;
                var report=parser.parse(provider,name,Files.readAllBytes(path));
                assertThat(report.rows()).as(name).isNotEmpty();
                assertThat(report.rows().stream().filter(row->row.valid()).count()).as(name).isPositive();
                files++;
            }
        }
        assertThat(files).isPositive();
    }
}
