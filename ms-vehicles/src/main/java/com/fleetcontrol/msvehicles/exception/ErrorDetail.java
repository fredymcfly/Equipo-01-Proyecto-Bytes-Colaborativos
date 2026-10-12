package com.fleetcontrol.msvehicles.exception;

/** One field-level validation failure. */
public record ErrorDetail(String field, String reason) {}
