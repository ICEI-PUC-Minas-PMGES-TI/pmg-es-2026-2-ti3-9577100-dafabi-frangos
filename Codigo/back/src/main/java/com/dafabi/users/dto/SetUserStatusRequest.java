package com.dafabi.users.dto;

import jakarta.validation.constraints.NotNull;

public record SetUserStatusRequest(@NotNull Boolean active) {}
