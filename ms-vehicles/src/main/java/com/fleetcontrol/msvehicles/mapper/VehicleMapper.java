package com.fleetcontrol.msvehicles.mapper;

import com.fleetcontrol.msvehicles.dto.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.VehicleResponse;
import com.fleetcontrol.msvehicles.dto.VehicleSummaryResponse;
import com.fleetcontrol.msvehicles.model.Vehicle;
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
