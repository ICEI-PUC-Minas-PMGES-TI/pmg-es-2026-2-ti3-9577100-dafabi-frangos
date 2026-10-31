package com.dafabi.integrations;

import com.dafabi.integrations.domain.IntegrationProvider;
import com.dafabi.integrations.dto.ImportBatchResponse;
import com.dafabi.integrations.application.IntegrationImportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntegrationImportIT {
    @Autowired IntegrationImportService imports;
    private MockMultipartFile report() throws Exception {
        try(XSSFWorkbook w=new XSSFWorkbook(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Sheet s=w.createSheet();
            String[] headers={"ID do loja","Nome do estabelecimento","ID do pedido","Horário do pedido","Horário da conclusão","Horário do cancelamento","Preço original do item","Receita real da loja"};
            String[] data={"123","Loja","1234567890123456789","2026-10-25 14:20:35","2026-10-25 14:50:27","","67,10","43,97"};
            Row h=s.createRow(0),r=s.createRow(1);
            for(int i=0;i<headers.length;i++){h.createCell(i).setCellValue(headers[i]);r.createCell(i).setCellValue(data[i]);}
            w.write(out);
            return new MockMultipartFile("file","pedidos.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",out.toByteArray());
        }
    }
    @Test void importsOnceAndFiltersByBusinessDate() throws Exception {
        MockMultipartFile file=report();
        ImportBatchResponse preview=imports.upload(IntegrationProvider.FOOD_99,file);
        assertThat(preview.validRowCount()).isEqualTo(1);
        assertThat(imports.orders(IntegrationProvider.FOOD_99,null,null)).isEmpty();
        imports.confirm(preview.id());
        imports.confirm(preview.id());
        assertThat(imports.upload(IntegrationProvider.FOOD_99,file).alreadyImported()).isTrue();
        assertThat(imports.orders(IntegrationProvider.FOOD_99,LocalDate.of(2026,10,25),LocalDate.of(2026,10,25)))
                .singleElement().satisfies(o->assertThat(o.externalOrderId()).isEqualTo("1234567890123456789"));
        assertThat(imports.orders(IntegrationProvider.FOOD_99,LocalDate.of(2026,10,26),LocalDate.of(2026,10,26))).isEmpty();
    }
}
