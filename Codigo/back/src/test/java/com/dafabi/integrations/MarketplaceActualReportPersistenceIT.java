package com.dafabi.integrations;

import com.dafabi.dashboard.application.FinancialDashboardService;
import com.dafabi.integrations.application.IntegrationImportService;
import com.dafabi.integrations.application.MarketplaceReportParser;
import com.dafabi.integrations.application.ParsedOrderRow;
import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.repository.MarketplaceOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceActualReportPersistenceIT {
    @Autowired IntegrationImportService imports;
    @Autowired FinancialDashboardService dashboard;
    @Autowired MarketplaceOrderRepository orders;
    @Autowired MarketplaceReportParser parser;

    @Test
    @EnabledIfEnvironmentVariable(named="DAFABI_REPORT_DIR",matches=".+")
    void importsReal99FoodOrdersAndUsesTheReportedNetOnce() throws Exception {
        String name="_Dados do pedido(20-10-2026-26-10-2026).zip";
        Path path=Path.of(System.getenv("DAFABI_REPORT_DIR"),name);
        assumeTrue(Files.isRegularFile(path));
        orders.deleteAll();
        var file=new MockMultipartFile("file",name,"application/zip",Files.readAllBytes(path));
        var preview=imports.upload(IntegrationProvider.FOOD_99,file);
        assertThat(preview.validRowCount()).isEqualTo(13);
        imports.confirm(preview.id());
        var result=dashboard.getSummary("custom",LocalDate.of(2026,10,20),LocalDate.of(2026,10,26));
        assertThat(result.summary().marketplaceNetReported()).isEqualByComparingTo("491.81");
        assertThat(result.summary().marketplaceOrdersWithoutNet()).isZero();
    }

    @Test
    @EnabledIfEnvironmentVariable(named="DAFABI_REPORT_DIR",matches=".+")
    void importsARealIfoodReportAndPreservesItsReportedNet() throws Exception {
        Path directory=Path.of(System.getenv("DAFABI_REPORT_DIR"));
        Path path;
        try(var paths=Files.list(directory)) {
            path=paths.filter(p->p.getFileName().toString().startsWith("relatorio-pedidos_")
                    && p.getFileName().toString().endsWith(".xlsx")).findFirst().orElse(null);
        }
        assumeTrue(path!=null);
        String name=path.getFileName().toString();
        byte[] content=Files.readAllBytes(path);
        var parsed=parser.parse(IntegrationProvider.IFOOD,name,content);
        BigDecimal expected=parsed.rows().stream().filter(row->row.valid()&&!row.duplicateInFile())
                .map(row->(ParsedOrderRow)row.payload())
                .filter(row->Set.of("COMPLETED","PARTIALLY_CANCELLED","CANCELLED").contains(row.normalizedStatus()))
                .map(ParsedOrderRow::netReported).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        orders.deleteAll();
        var preview=imports.upload(IntegrationProvider.IFOOD,new MockMultipartFile("file",name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",content));
        imports.confirm(preview.id());
        var result=dashboard.getSummary("custom",parsed.start(),parsed.end());
        assertThat(result.summary().marketplaceNetReported()).isEqualByComparingTo(expected);
    }
}
