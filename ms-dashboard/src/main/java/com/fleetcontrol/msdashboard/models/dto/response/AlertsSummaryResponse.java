package com.fleetcontrol.msdashboard.models.dto.response;

/** Open alerts, in total and by severity. */
public record AlertsSummaryResponse(long open, long critical, long warning, long info) {}
