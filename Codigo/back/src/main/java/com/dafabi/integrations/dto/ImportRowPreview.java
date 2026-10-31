package com.dafabi.integrations.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportRowPreview(
        int rowNumber,
        String externalId,
        LocalDate date,
        String status,
        String itemName,
        BigDecimal quantity,
        BigDecimal gross,
        BigDecimal netReported,
        boolean valid,
        boolean duplicateInFile,
        boolean duplicateInSystem,
        List<String> issues) {
}
