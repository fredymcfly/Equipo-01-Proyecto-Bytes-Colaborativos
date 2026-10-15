package com.fleetcontrol.msmaintenance.mapper;

import com.fleetcontrol.msmaintenance.models.dto.response.OrderResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Maps maintenance orders to their DTOs. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MaintenanceOrderMapper {

  OrderResponse toResponse(MaintenanceOrder order);
}
