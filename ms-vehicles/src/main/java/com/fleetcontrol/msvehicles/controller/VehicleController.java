package com.fleetcontrol.msvehicles.controller;

import com.fleetcontrol.msvehicles.dto.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.PageResponse;
import com.fleetcontrol.msvehicles.dto.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.VehicleResponse;
import com.fleetcontrol.msvehicles.dto.VehicleStatusRequest;
import com.fleetcontrol.msvehicles.dto.VehicleSummaryResponse;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
import com.fleetcontrol.msvehicles.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the vehicle catalogue. */
@RestController
@RequestMapping("/api/vehicles")
@Validated
@Tag(name = "Vehicles")
@RequiredArgsConstructor
public class VehicleController {

  private final VehicleService vehicleService;

  /** Lists vehicles with optional filters and pagination. */
  @GetMapping
  @Operation(summary = "List vehicles")
  public PageResponse<VehicleSummaryResponse> list(
      @RequestParam(required = false) VehicleStatus status,
      @RequestParam(required = false) VehicleType type,
      @RequestParam(required = false) String plate,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return vehicleService.list(status, type, plate, page, size);
  }

  /** Returns the detailed data of one vehicle. */
  @GetMapping("/{vehicleId}")
  @Operation(summary = "Get a vehicle by id")
  public VehicleResponse get(@PathVariable UUID vehicleId) {
    return vehicleService.getById(vehicleId);
  }

  /** Registers a new vehicle. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a vehicle")
  public VehicleResponse create(@Valid @RequestBody CreateVehicleRequest request) {
    return vehicleService.create(request);
  }

  /** Updates the descriptive data of a vehicle. */
  @PutMapping("/{vehicleId}")
  @Operation(summary = "Update a vehicle")
  public VehicleResponse update(
      @PathVariable UUID vehicleId, @Valid @RequestBody UpdateVehicleRequest request) {
    return vehicleService.update(vehicleId, request);
  }

  /** Changes the status of a vehicle and, optionally, its odometer. */
  @PatchMapping("/{vehicleId}/status")
  @Operation(summary = "Change the status and odometer of a vehicle")
  public VehicleResponse changeStatus(
      @PathVariable UUID vehicleId, @Valid @RequestBody VehicleStatusRequest request) {
    return vehicleService.changeStatus(vehicleId, request);
  }
}
