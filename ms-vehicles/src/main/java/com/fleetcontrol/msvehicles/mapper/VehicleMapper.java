package com.fleetcontrol.msvehicles.mapper;

import com.fleetcontrol.msvehicles.models.dto.request.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.models.dto.request.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.models.dto.response.VehicleResponse;
import com.fleetcontrol.msvehicles.models.dto.response.VehicleSummaryResponse;
import com.fleetcontrol.msvehicles.models.entities.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/** Maps between vehicle entities and their DTOs. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VehicleMapper {

  VehicleResponse toResponse(Vehicle vehicle);

  VehicleSummaryResponse toSummary(Vehicle vehicle);

  Vehicle toEntity(CreateVehicleRequest request);

  void applyUpdate(UpdateVehicleRequest request, @MappingTarget Vehicle vehicle);
}
