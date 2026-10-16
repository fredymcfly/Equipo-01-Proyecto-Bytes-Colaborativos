package com.fleetcontrol.msdashboard.models.dto.response;

import java.math.BigDecimal;

/**
 * Value of a metric in one period.
 *
 * @param period first day of the period, as returned by the source service
 * @param value value of the metric, or {@code null} when the period has no data
 */
public record SeriesPointResponse(String period, BigDecimal value) {}
