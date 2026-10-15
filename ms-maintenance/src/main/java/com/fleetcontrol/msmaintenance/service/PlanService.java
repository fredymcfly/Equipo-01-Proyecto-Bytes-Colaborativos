package com.fleetcontrol.msmaintenance.service;

import com.fleetcontrol.msmaintenance.client.VehicleGateway;
import com.fleetcontrol.msmaintenance.exception.PlanAlreadyExistsException;
import com.fleetcontrol.msmaintenance.mapper.MaintenancePlanMapper;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.dto.request.CreatePlanRequest;
import com.fleetcontrol.msmaintenance.models.dto.response.PageResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.PlanResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenancePlanRepository;
import com.fleetcontrol.msmaintenance.repository.specifications.PlanSpecifications;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** Business logic for maintenance plans. */
@Service
@RequiredArgsConstructor
public class PlanService {

  private final MaintenancePlanRepository repository;
  private final MaintenancePlanMapper mapper;
  private final VehicleGateway vehicleGateway;
  private final Clock clock;

  /**
   * Creates a plan for a vehicle that exists in ms-vehicles. When the last intervention is not
   * given, today and the current odometer of the vehicle are used.
   */
  public PlanResponse create(CreatePlanRequest request) {
    VehicleResponse vehicle = vehicleGateway.getVehicle(request.vehicleId());
    if (repository.existsByVehicleIdAndTypeAndActiveTrue(request.vehicleId(), request.type())) {
      throw new PlanAlreadyExistsException();
    }
    LocalDate lastDoneAt =
        request.lastDoneAt() != null ? request.lastDoneAt() : LocalDate.now(clock);
    Integer lastDoneKm = request.lastDoneKm() != null ? request.lastDoneKm() : vehicle.odometerKm();

    MaintenancePlan plan = new MaintenancePlan();
    plan.setVehicleId(request.vehicleId());
    plan.setType(request.type());
    plan.setIntervalKm(request.intervalKm());
    plan.setIntervalDays(request.intervalDays());
    plan.registerDone(lastDoneAt, lastDoneKm == null ? 0 : lastDoneKm);
    try {
      return mapper.toResponse(repository.saveAndFlush(plan));
    } catch (DataIntegrityViolationException exception) {
      throw new PlanAlreadyExistsException();
    }
  }

  /** Lists plans ordered by due date, with the plans that only depend on kilometers last. */
  public PageResponse<PlanResponse> list(
      UUID vehicleId, MaintenanceType type, Boolean active, int page, int size) {
    Specification<MaintenancePlan> filters =
        PlanSpecifications.hasVehicle(vehicleId)
            .and(PlanSpecifications.hasType(type))
            .and(PlanSpecifications.hasActive(active));
    // PostgreSQL sorts NULLs last in ascending order: plans without a due date go at the end.
    Sort order =
        Sort.by(Sort.Order.asc("nextDueAt"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
    return PageResponse.from(
        repository.findAll(filters, PageRequest.of(page, size, order)).map(mapper::toResponse));
  }
}
