package com.fleetcontrol.msdashboard.models.dto.response;

/** Number of vehicles of the fleet, in total and by status. */
public record FleetStatusResponse(
    long total, long available, long inUse, long inMaintenance, long outOfService) {}
