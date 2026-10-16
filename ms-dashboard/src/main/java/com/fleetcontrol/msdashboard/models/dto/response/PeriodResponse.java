package com.fleetcontrol.msdashboard.models.dto.response;

import java.time.LocalDate;

/** Dates covered by a response, both included. */
public record PeriodResponse(LocalDate from, LocalDate to) {}
