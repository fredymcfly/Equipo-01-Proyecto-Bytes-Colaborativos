package com.fleetcontrol.msfuel.service;

import static com.fleetcontrol.msfuel.exception.ErrorCode.INVALID_ODOMETER;
import static com.fleetcontrol.msfuel.exception.ErrorCode.VEHICLE_NOT_FOUND;

import com.fleetcontrol.msfuel.client.VehicleClient;
import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.dto.FuelConsumptionResponse;
import com.fleetcontrol.msfuel.dto.FuelRecordRequest;
import com.fleetcontrol.msfuel.dto.FuelRecordResponse;
import com.fleetcontrol.msfuel.dto.FuelStatsPeriod;
import com.fleetcontrol.msfuel.dto.FuelStatsResponse;
import com.fleetcontrol.msfuel.dto.Granularity;
import com.fleetcontrol.msfuel.dto.PageResponse;
import com.fleetcontrol.msfuel.dto.StatsTotals;
import com.fleetcontrol.msfuel.dto.VehicleConsumption;
import com.fleetcontrol.msfuel.exception.BusinessConflictException;
import com.fleetcontrol.msfuel.exception.InvalidRequestException;
import com.fleetcontrol.msfuel.exception.ResourceNotFoundException;
import com.fleetcontrol.msfuel.mapper.FuelRecordMapper;
import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.model.FuelType;
import com.fleetcontrol.msfuel.repository.FuelRecordRepository;
import com.fleetcontrol.msfuel.repository.projection.FuelPeriodAggregate;
import com.fleetcontrol.msfuel.repository.projection.FuelTotalsAggregate;
import com.fleetcontrol.msfuel.repository.projection.VehicleFuelAggregate;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra repostajes y calcula el consumo entre depósitos llenos. */
@Service
@RequiredArgsConstructor
public class FuelService {

  private static final int SCALE = 2;

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
        request.liters().multiply(request.pricePerLiter()).setScale(SCALE, RoundingMode.HALF_UP));
    fuelRecord.setConsumptionL100km(consumptionFor(request));

    return mapper.toResponse(repository.save(fuelRecord));
  }

  /**
   * Repostajes de la flota o de un vehículo, filtrados por fecha y paginados por fecha descendente.
   *
   * @param vehicle vehículo a filtrar, o {@code null} para toda la flota
   * @param from fecha inicial incluida, o {@code null}
   * @param to fecha final incluida, o {@code null}
   * @param page número de página, desde cero
   * @param size tamaño de la página
   * @return la página de repostajes
   */
  @Transactional(readOnly = true)
  public PageResponse<FuelRecordResponse> list(
      UUID vehicle, LocalDate from, LocalDate to, int page, int size) {
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "refueledAt"));
    Page<FuelRecord> records = findPage(vehicle, from, to, pageable);
    return PageResponse.from(records.map(mapper::toResponse));
  }

  /**
   * Litros, coste, distancia y consumo medio de cada vehículo dentro de un periodo.
   *
   * @param from fecha inicial incluida
   * @param to fecha final incluida
   * @return el consumo por vehículo, ordenado por coste descendente
   */
  @Transactional(readOnly = true)
  public FuelConsumptionResponse consumption(LocalDate from, LocalDate to) {
    validateRange(from, to);
    List<VehicleConsumption> vehicles =
        repository.aggregateByVehicle(startOfDay(from), startOfDay(to.plusDays(1))).stream()
            .map(this::toConsumption)
            .toList();
    return new FuelConsumptionResponse(from, to, vehicles);
  }

  /**
   * Serie temporal de litros, coste y consumo, con los periodos sin datos a cero.
   *
   * @param from fecha inicial incluida
   * @param to fecha final incluida
   * @param granularity tamaño del periodo
   * @param vehicle vehículo a filtrar, o {@code null} para toda la flota
   * @return la serie y sus totales
   */
  @Transactional(readOnly = true)
  public FuelStatsResponse stats(
      LocalDate from, LocalDate to, Granularity granularity, UUID vehicle) {
    validateRange(from, to);
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));

    List<FuelPeriodAggregate> aggregates =
        vehicle == null
            ? repository.aggregateByPeriod(start, end, granularity.unit())
            : repository.aggregateByPeriodAndVehicle(start, end, granularity.unit(), vehicle);
    FuelTotalsAggregate totals =
        vehicle == null
            ? repository.totals(start, end)
            : repository.totalsByVehicle(start, end, vehicle);

    Map<LocalDate, FuelPeriodAggregate> byPeriod =
        aggregates.stream()
            .collect(Collectors.toMap(a -> LocalDate.parse(a.period()), Function.identity()));

    List<FuelStatsPeriod> series = new ArrayList<>();
    for (LocalDate cursor = align(from, granularity);
        !cursor.isAfter(to);
        cursor = advance(cursor, granularity)) {
      series.add(toStatsPeriod(cursor, byPeriod.get(cursor)));
    }

    return new FuelStatsResponse(from, to, granularity, toTotals(totals), series);
  }

  private Page<FuelRecord> findPage(
      UUID vehicle, LocalDate from, LocalDate to, PageRequest pageable) {
    if (vehicle == null && from == null && to == null) {
      return repository.findAll(pageable);
    }
    if (from == null || to == null) {
      throw new InvalidRequestException("from and to must be provided together");
    }
    validateRange(from, to);
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    if (vehicle == null) {
      return repository.findByRefueledAtBetween(start, end, pageable);
    }
    return repository.findByVehicleIdAndRefueledAtBetween(vehicle, start, end, pageable);
  }

  private VehicleConsumption toConsumption(VehicleFuelAggregate aggregate) {
    VehicleResponse vehicle = findVehicle(aggregate.vehicleId());
    return new VehicleConsumption(
        aggregate.vehicleId(),
        vehicle.plate(),
        vehicle.type(),
        aggregate.liters(),
        aggregate.cost(),
        distanceOf(aggregate),
        scaleOrNull(aggregate.avgConsumptionL100km()));
  }

  private static Integer distanceOf(VehicleFuelAggregate aggregate) {
    if (aggregate.minOdometerKm() == null || aggregate.maxOdometerKm() == null) {
      return null;
    }
    return aggregate.maxOdometerKm() - aggregate.minOdometerKm();
  }

  private static FuelStatsPeriod toStatsPeriod(LocalDate period, FuelPeriodAggregate aggregate) {
    if (aggregate == null) {
      return new FuelStatsPeriod(
          period, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    return new FuelStatsPeriod(
        period,
        aggregate.liters(),
        aggregate.cost(),
        scaleOrNull(aggregate.avgPricePerLiter()),
        scaleOrNull(aggregate.avgConsumptionL100km()));
  }

  private static StatsTotals toTotals(FuelTotalsAggregate totals) {
    return new StatsTotals(
        zeroIfNull(totals.liters()),
        zeroIfNull(totals.cost()),
        scaleZero(totals.avgPricePerLiter()),
        scaleZero(totals.avgConsumptionL100km()));
  }

  private static void validateRange(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new InvalidRequestException("from and to are required");
    }
    if (from.isAfter(to)) {
      throw new InvalidRequestException("from must not be after to");
    }
  }

  private static Instant startOfDay(LocalDate date) {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private static LocalDate align(LocalDate date, Granularity granularity) {
    return switch (granularity) {
      case DAY -> date;
      case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> date.withDayOfMonth(1);
    };
  }

  private static LocalDate advance(LocalDate date, Granularity granularity) {
    return switch (granularity) {
      case DAY -> date.plusDays(1);
      case WEEK -> date.plusWeeks(1);
      case MONTH -> date.plusMonths(1);
    };
  }

  private static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static BigDecimal scaleOrNull(BigDecimal value) {
    return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
  }

  private static BigDecimal scaleZero(BigDecimal value) {
    return zeroIfNull(value).setScale(SCALE, RoundingMode.HALF_UP);
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
          "The liters exceed the tank capacity (" + vehicle.tankCapacityL() + " L)");
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

    return partials.multiply(BigDecimal.valueOf(100)).divide(km, SCALE, RoundingMode.HALF_UP);
  }
}
