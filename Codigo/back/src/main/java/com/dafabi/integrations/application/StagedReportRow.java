package com.dafabi.integrations.application;

import java.util.List;

public record StagedReportRow(int rowNumber, String rowKey, boolean valid,
        boolean duplicateInFile, boolean duplicateInSystem, List<String> issues, Object payload) {
}
