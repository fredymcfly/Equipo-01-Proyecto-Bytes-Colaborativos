package com.fleetcontrol.msmaintenance.models.dto.response;

/** One field-level validation failure. */
public record ErrorDetail(String field, String reason) {}
