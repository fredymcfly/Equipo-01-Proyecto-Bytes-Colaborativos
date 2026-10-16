package com.fleetcontrol.msvehicles.models.dto.response;

/** One field-level validation failure. */
public record ErrorDetail(String field, String reason) {}
