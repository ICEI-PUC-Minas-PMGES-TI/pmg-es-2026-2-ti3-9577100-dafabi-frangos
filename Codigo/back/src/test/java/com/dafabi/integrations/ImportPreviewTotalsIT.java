package com.dafabi.integrations;

import com.dafabi.integrations.application.IntegrationImportService;
import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.repository.MarketplaceOrderRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportPreviewTotalsIT {

    private static final List<String> HEADERS = List.of("ID COMPLETO DO PEDIDO", "ID DA LOJA", "NOME DA LOJA",
            "DATA E HORA DO PEDIDO", "STATUS FINAL DO PEDIDO", "VALOR DOS ITENS (R$)", "TOTAL PAGO PELO CLIENTE (R$)",
            "TAXAS E COMISSÕES (R$)", "VALOR LÍQUIDO (R$)");

    @Autowired IntegrationImportService imports;
    @Autowired MarketplaceOrderRepository orders;

    @Test
    void previewTotalsCountOnlyOrdersThatAreNotImportedYet() throws Exception {
        orders.deleteAll();
        var first = imports.upload(IntegrationProvider.IFOOD, report("pedidos-1.xlsx",
                List.of("PEDIDO-A", "10/11/2026 12:00:00", "107,90")));
        imports.confirm(first.id());

        var second = imports.upload(IntegrationProvider.IFOOD, report("pedidos-2.xlsx",
                List.of("PEDIDO-A", "10/11/2026 12:00:00", "107,90"),
                List.of("PEDIDO-B", "11/11/2026 12:00:00", "1,00")));

        assertThat(second.duplicateRowCount()).isEqualTo(1);
        assertThat(second.summary().grossItems()).isEqualByComparingTo("1.00");
        assertThat(second.summary().completedOrders()).isEqualTo(1);
    }

    @SafeVarargs
    private static MockMultipartFile report(String name, List<String>... orders) throws Exception {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Pedidos");
            var header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) header.createCell(i).setCellValue(HEADERS.get(i));
            int line = 1;
            for (List<String> order : orders) {
                var row = sheet.createRow(line++);
                List<String> values = List.of(order.get(0), "3349794", "DaFabi Frangos", order.get(1), "CONCLUIDO",
                        order.get(2), order.get(2), "-0,20", order.get(2));
                for (int i = 0; i < values.size(); i++) row.createCell(i).setCellValue(values.get(i));
            }
            workbook.write(out);
            return new MockMultipartFile("file", name,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
}
