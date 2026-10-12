package com.fleetcontrol.msvehicles.service;

import com.fleetcontrol.msvehicles.dto.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.PageResponse;
import com.fleetcontrol.msvehicles.dto.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.VehicleResponse;
import com.fleetcontrol.msvehicles.dto.VehicleStatusRequest;
import com.fleetcontrol.msvehicles.dto.VehicleSummaryResponse;
import com.fleetcontrol.msvehicles.exception.ForbiddenException;
import com.fleetcontrol.msvehicles.exception.InvalidOdometerException;
import com.fleetcontrol.msvehicles.exception.InvalidStatusTransitionException;
import com.fleetcontrol.msvehicles.exception.VehicleAlreadyExistsException;
import com.fleetcontrol.msvehicles.exception.VehicleNotFoundException;
import com.fleetcontrol.msvehicles.mapper.VehicleMapper;
import com.fleetcontrol.msvehicles.model.Vehicle;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
import com.fleetcontrol.msvehicles.repository.VehicleRepository;
import com.fleetcontrol.msvehicles.repository.VehicleSpecifications;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for the vehicle catalogue. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VehicleService {

  private static final Map<VehicleStatus, Set<VehicleStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          VehicleStatus.AVAILABLE,
          EnumSet.of(
              VehicleStatus.IN_USE, VehicleStatus.IN_MAINTENANCE, VehicleStatus.OUT_OF_SERVICE),
          VehicleStatus.IN_USE,
          EnumSet.of(VehicleStatus.AVAILABLE, VehicleStatus.OUT_OF_SERVICE),
          VehicleStatus.IN_MAINTENANCE,
          EnumSet.of(VehicleStatus.AVAILABLE, VehicleStatus.OUT_OF_SERVICE),
          VehicleStatus.OUT_OF_SERVICE,
          EnumSet.of(VehicleStatus.AVAILABLE));

  private final VehicleRepository repository;
  private final VehicleMapper mapper;

  /** Registers a new vehicle, which always starts as available. */
  @Transactional
  public VehicleResponse create(CreateVehicleRequest request) {
    if (repository.existsByPlate(request.plate())) {
      throw new VehicleAlreadyExistsException();
    }
    Vehicle vehicle = mapper.toEntity(request);
    vehicle.setStatus(VehicleStatus.AVAILABLE);
    return mapper.toResponse(repository.save(vehicle));
  }

  /** Returns a page of vehicles, optionally filtered by status, type or plate. */
  public PageResponse<VehicleSummaryResponse> list(
      VehicleStatus status, VehicleType type, String plate, int page, int size) {
    Specification<Vehicle> filters =
        VehicleSpecifications.hasStatus(status)
            .and(VehicleSpecifications.hasType(type))
            .and(VehicleSpecifications.plateContains(plate));
    Page<VehicleSummaryResponse> result =
        repository.findAll(filters, PageRequest.of(page, size)).map(mapper::toSummary);
    return PageResponse.from(result);
  }

  /** Returns the detailed data of one vehicle. */
  public VehicleResponse getById(UUID vehicleId) {
    return mapper.toResponse(findVehicle(vehicleId));
  }

  /** Updates the descriptive data of a vehicle. */
  @Transactional
  public VehicleResponse update(UUID vehicleId, UpdateVehicleRequest request) {
    Vehicle vehicle = findVehicle(vehicleId);
    mapper.applyUpdate(request, vehicle);
    return mapper.toResponse(repository.save(vehicle));
  }

  /** Changes the status of a vehicle and, optionally, its odometer. */
  @Transactional
  public VehicleResponse changeStatus(UUID vehicleId, VehicleStatusRequest request) {
    Vehicle vehicle = findVehicle(vehicleId);
    VehicleStatus current = vehicle.getStatus();
    VehicleStatus target = request.status();
    if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
      throw new InvalidStatusTransitionException();
    }
    if (current == VehicleStatus.OUT_OF_SERVICE
        && target == VehicleStatus.AVAILABLE
        && !isAdmin()) {
      throw new ForbiddenException();
    }
    if (request.odometerKm() != null) {
      if (request.odometerKm() < vehicle.getOdometerKm()) {
        throw new InvalidOdometerException();
      }
      vehicle.setOdometerKm(request.odometerKm());
    }
    vehicle.setStatus(target);
    return mapper.toResponse(repository.save(vehicle));
  }

  private static boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
  }

  private Vehicle findVehicle(UUID vehicleId) {
    return repository.findById(vehicleId).orElseThrow(VehicleNotFoundException::new);
  }
}
