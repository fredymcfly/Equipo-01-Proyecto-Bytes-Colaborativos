package com.fleetcontrol.msdrivers.mapper;

import com.fleetcontrol.msdrivers.models.dto.request.CreateDriverRequest;
import com.fleetcontrol.msdrivers.models.dto.request.UpdateDriverRequest;
import com.fleetcontrol.msdrivers.models.dto.response.DriverResponse;
import com.fleetcontrol.msdrivers.models.dto.response.DriverSummaryResponse;
import com.fleetcontrol.msdrivers.models.entities.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/** Maps between driver entities and their DTOs. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DriverMapper {

  DriverResponse toResponse(Driver driver);

  DriverSummaryResponse toSummary(Driver driver);

  Driver toEntity(CreateDriverRequest request);

  void applyUpdate(UpdateDriverRequest request, @MappingTarget Driver driver);
}
