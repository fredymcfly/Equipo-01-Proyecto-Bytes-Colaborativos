package com.fleetcontrol.msalerts.models.dto.response;

import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.util.List;

/**
 * Result of an evaluation.
 *
 * @param created alerts that did not exist and were created
 * @param resolved active alerts whose condition no longer holds
 * @param skipped types that were not evaluated because a source service did not answer
 * @param durationMs time the evaluation took
 */
public record EvaluationResponse(
    int created, int resolved, List<AlertType> skipped, long durationMs) {}
