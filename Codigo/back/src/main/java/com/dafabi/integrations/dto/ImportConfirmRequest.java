package com.dafabi.integrations.dto;

import java.util.List;
import java.util.UUID;

public record ImportConfirmRequest(List<UUID> omittedItemIdsToZero) {}
