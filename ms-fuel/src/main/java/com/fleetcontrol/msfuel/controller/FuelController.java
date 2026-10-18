package com.fleetcontrol.msfuel.controller;

import com.fleetcontrol.msfuel.dto.FuelConsumptionResponse;
import com.fleetcontrol.msfuel.dto.FuelRecordRequest;
import com.fleetcontrol.msfuel.dto.FuelRecordResponse;
import com.fleetcontrol.msfuel.dto.FuelStatsResponse;
import com.fleetcontrol.msfuel.dto.Granularity;
import com.fleetcontrol.msfuel.dto.PageResponse;
import com.fleetcontrol.msfuel.service.FuelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Repostajes de la flota y métricas de consumo. */
@RestController
@RequestMapping("/api/fuel")
@RequiredArgsConstructor
@Validated
@Tag(name = "Fuel", description = "Fleet refuels and fuel consumption")
@SecurityRequirement(name = "bearerAuth")
public class FuelController {

  private static final String DATE_FORMAT = "yyyy-MM-dd";

  private final FuelService fuelService;

  /** Registra un repostaje y devuelve el consumo calculado. */
  @PostMapping("/refuels")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registers a refuel")
  @ApiResponse(responseCode = "201", description = "Refuel registered")
  @ApiResponse(responseCode = "400", description = "Invalid liters or above the tank capacity")
  @ApiResponse(responseCode = "404", description = "The vehicle does not exist")
  @ApiResponse(responseCode = "409", description = "Odometer lower than the previous refuel")
  @ApiResponse(responseCode = "503", description = "ms-vehicles does not answer")
  public FuelRecordResponse create(@Valid @RequestBody FuelRecordRequest request) {
    return fuelService.saveReFuel(request);
  }

  /** Lista paginada de repostajes, filtrable por vehículo y fechas. */
  @GetMapping("/refuels")
  @Operation(summary = "Lists refuels")
  public PageResponse<FuelRecordResponse> list(
      @RequestParam(required = false) UUID vehicle,
      @RequestParam(required = false) @DateTimeFormat(pattern = DATE_FORMAT) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(pattern = DATE_FORMAT) LocalDate to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return fuelService.list(vehicle, from, to, page, size);
  }

  /** Consumo agregado por vehículo en un periodo. */
  @GetMapping("/consumption")
  @Operation(summary = "Fuel consumption by vehicle")
  public FuelConsumptionResponse consumption(
      @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDate from,
      @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDate to) {
    return fuelService.consumption(from, to);
  }

  /** Serie temporal de litros, coste y consumo. */
  @GetMapping("/stats")
  @Operation(summary = "Time series of liters, cost and consumption")
  public FuelStatsResponse stats(
      @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDate from,
      @RequestParam @DateTimeFormat(pattern = DATE_FORMAT) LocalDate to,
      @RequestParam(defaultValue = "DAY") Granularity granularity,
      @RequestParam(required = false) UUID vehicle) {
    return fuelService.stats(from, to, granularity, vehicle);
  }
}
