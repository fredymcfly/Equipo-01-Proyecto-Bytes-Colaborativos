package com.fleetcontrol.msfuel.mapper;

import com.fleetcontrol.msfuel.dto.FuelRecordRequest;
import com.fleetcontrol.msfuel.dto.FuelRecordResponse;
import com.fleetcontrol.msfuel.model.FuelRecord;
import org.mapstruct.Mapper;

/** Convierte repostajes de la entidad a la respuesta de la API. */
@Mapper(componentModel = "spring")
public interface FuelRecordMapper {

  /** Proyecta la entidad sobre el DTO con los mismos nombres de campo. */
  FuelRecordResponse toResponse(FuelRecord fuelRecord);

  FuelRecord toEntity(FuelRecordRequest fuelRecordRequest);
}
