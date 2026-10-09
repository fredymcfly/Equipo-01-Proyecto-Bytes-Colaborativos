package com.fleetcontrol.msvehicles.service;

import com.fleetcontrol.msvehicles.dto.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.PageResponse;
import com.fleetcontrol.msvehicles.dto.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.VehicleResponse;
import com.fleetcontrol.msvehicles.dto.VehicleSummaryResponse;
import com.fleetcontrol.msvehicles.exception.VehicleAlreadyExistsException;
import com.fleetcontrol.msvehicles.exception.VehicleNotFoundException;
import com.fleetcontrol.msvehicles.mapper.VehicleMapper;
import com.fleetcontrol.msvehicles.model.Vehicle;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
import com.fleetcontrol.msvehicles.repository.VehicleRepository;
import com.fleetcontrol.msvehicles.repository.VehicleSpecifications;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for the vehicle catalogue. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VehicleService {

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

  private Vehicle findVehicle(UUID vehicleId) {
    return repository.findById(vehicleId).orElseThrow(VehicleNotFoundException::new);
  }
}
