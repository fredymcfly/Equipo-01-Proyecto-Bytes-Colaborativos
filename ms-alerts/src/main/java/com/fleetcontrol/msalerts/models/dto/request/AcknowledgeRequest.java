package com.fleetcontrol.msalerts.models.dto.request;

import com.fleetcontrol.msalerts.constants.BusinessConstants;
import jakarta.validation.constraints.Size;

/** Optional body of the acknowledgement of an alert. */
public record AcknowledgeRequest(
    @Size(max = BusinessConstants.FieldLimits.COMMENT_MAX) String comment) {}
