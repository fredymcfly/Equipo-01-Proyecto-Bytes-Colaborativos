package com.fleetcontrol.msfuel.service;

import static com.fleetcontrol.msfuel.exception.ErrorCode.INVALID_ODOMETER;
import static com.fleetcontrol.msfuel.exception.ErrorCode.VEHICLE_NOT_FOUND;

import com.fleetcontrol.msfuel.client.VehicleClient;
import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.dto.FuelRecordRequest;
import com.fleetcontrol.msfuel.dto.FuelRecordResponse;
import com.fleetcontrol.msfuel.exception.BusinessConflictException;
import com.fleetcontrol.msfuel.exception.InvalidRequestException;
import com.fleetcontrol.msfuel.exception.ResourceNotFoundException;
import com.fleetcontrol.msfuel.mapper.FuelRecordMapper;
import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.model.FuelType;
import com.fleetcontrol.msfuel.repository.FuelRecordRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra repostajes y calcula el consumo entre depósitos llenos. */
@Service
@RequiredArgsConstructor
public class FuelService {

  private final FuelRecordRepository repository;
  private final VehicleClient vehicleClient;
  private final FuelRecordMapper mapper;

  /**
   * Registra un repostaje tras validar el vehículo, la capacidad del depósito y el odómetro.
   *
   * @param request datos del repostaje
   * @return el repostaje guardado, con su coste total y su consumo calculados
   */
  @Transactional
  public FuelRecordResponse saveReFuel(FuelRecordRequest request) {
    VehicleResponse vehicle = findVehicle(request.vehicleId());

    validateCapacity(request, vehicle);
    validateOdometer(request);

    FuelRecord fuelRecord = mapper.toEntity(request);
    fuelRecord.setFuelType(FuelType.valueOf(vehicle.fuelType()));
    fuelRecord.setTotalCost(
        request.liters().multiply(request.pricePerLiter()).setScale(2, RoundingMode.HALF_UP));
    fuelRecord.setConsumptionL100km(consumptionFor(request));

    return mapper.toResponse(repository.save(fuelRecord));
  }

  /** El vehículo no puede ser null: Feign falla con excepción o con el fallback. */
  private VehicleResponse findVehicle(UUID vehicleId) {
    try {
      return vehicleClient.getVehicle(vehicleId);
    } catch (FeignException ex) {
      if (ex.status() == 404) {
        throw new ResourceNotFoundException(VEHICLE_NOT_FOUND);
      }
      throw ex;
    }
  }

  /** El repostaje no puede superar la capacidad del depósito, que son litros enteros. */
  private void validateCapacity(FuelRecordRequest request, VehicleResponse vehicle) {
    if (request.liters().compareTo(BigDecimal.valueOf(vehicle.tankCapacityL())) > 0) {
      throw new InvalidRequestException(
          "Los litros superan la capacidad del depósito (" + vehicle.tankCapacityL() + " L)");
    }
  }

  /** El odómetro nunca puede retroceder respecto al repostaje más avanzado del vehículo. */
  private void validateOdometer(FuelRecordRequest request) {
    FuelRecord last =
        repository.findFirstByVehicleIdOrderByOdometerKmDesc(request.vehicleId()).orElse(null);
    if (last != null && request.odometerKm() < last.getOdometerKm()) {
      throw new BusinessConflictException(INVALID_ODOMETER);
    }
  }

  /** Solo los llenos cierran un intervalo; los parciales no tienen consumo. */
  private BigDecimal consumptionFor(FuelRecordRequest request) {
    if (!Boolean.TRUE.equals(request.fullTank())) {
      return null;
    }
    return calculateConsumption(request.vehicleId(), request.odometerKm(), request.liters());
  }

  private BigDecimal calculateConsumption(UUID vehicleId, Integer odometerKm, BigDecimal liters) {
    return repository
        .findFirstByVehicleIdAndFullTankTrueAndOdometerKmLessThanOrderByOdometerKmDesc(
            vehicleId, odometerKm)
        .map(previous -> consumptionBetween(previous, odometerKm, liters))
        .orElse(null);
  }

  /** (litros desde el lleno anterior / km entre llenos) × 100, con dos decimales. */
  private BigDecimal consumptionBetween(
      FuelRecord previous, Integer odometerKm, BigDecimal liters) {
    BigDecimal km = BigDecimal.valueOf(odometerKm - previous.getOdometerKm());
    if (km.signum() <= 0) {
      return null;
    }
    BigDecimal partials =
        repository
            .findByVehicleIdAndOdometerKmGreaterThanAndOdometerKmLessThanOrderByOdometerKmAsc(
                previous.getVehicleId(), previous.getOdometerKm(), odometerKm)
            .stream()
            .map(FuelRecord::getLiters)
            .reduce(liters, BigDecimal::add);

    return partials
        .divide(km, 2, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .setScale(2, RoundingMode.HALF_UP);
  }
}
