package com.fleetcontrol.msmaintenance.mapper;

import com.fleetcontrol.msmaintenance.models.dto.response.PlanResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Maps maintenance plans to their DTOs. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MaintenancePlanMapper {

  PlanResponse toResponse(MaintenancePlan plan);
}
