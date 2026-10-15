package com.fleetcontrol.msmaintenance.service;

import com.fleetcontrol.msmaintenance.client.VehicleGateway;
import com.fleetcontrol.msmaintenance.constants.BusinessConstants;
import com.fleetcontrol.msmaintenance.exception.InvalidOdometerException;
import com.fleetcontrol.msmaintenance.exception.InvalidOrderStateException;
import com.fleetcontrol.msmaintenance.exception.OrderNotFoundException;
import com.fleetcontrol.msmaintenance.exception.VehicleNotAvailableException;
import com.fleetcontrol.msmaintenance.mapper.MaintenanceOrderMapper;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.dto.request.CompleteOrderRequest;
import com.fleetcontrol.msmaintenance.models.dto.response.CompleteOrderResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.OrderResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.PageResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenanceOrderRepository;
import com.fleetcontrol.msmaintenance.repository.MaintenancePlanRepository;
import com.fleetcontrol.msmaintenance.repository.specifications.OrderSpecifications;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Business logic for maintenance orders. */
@Service
@RequiredArgsConstructor
public class OrderService {

  private final MaintenanceOrderRepository orderRepository;
  private final MaintenancePlanRepository planRepository;
  private final MaintenanceOrderMapper orderMapper;
  private final VehicleGateway vehicleGateway;
  private final TransactionTemplate transactionTemplate;
  private final Clock clock;

  /** Lists orders ordered by their scheduled date, with optional filters. */
  public PageResponse<OrderResponse> list(
      UUID vehicleId,
      MaintenanceOrderStatus status,
      MaintenanceType type,
      LocalDate dueBefore,
      int page,
      int size) {
    Specification<MaintenanceOrder> filters =
        OrderSpecifications.hasVehicle(vehicleId)
            .and(OrderSpecifications.hasStatus(status))
            .and(OrderSpecifications.hasType(type))
            .and(OrderSpecifications.scheduledOnOrBefore(dueBefore));
    Sort order = Sort.by(Sort.Order.asc("scheduledFor"), Sort.Order.asc("id"));
    return PageResponse.from(
        orderRepository
            .findAll(filters, PageRequest.of(page, size, order))
            .map(orderMapper::toResponse));
  }

  /**
   * Starts a pending order and puts the vehicle in maintenance. The order only changes once
   * ms-vehicles has accepted the change.
   */
  public OrderResponse start(UUID orderId) {
    MaintenanceOrder order = findOrder(orderId);
    if (order.getStatus() != MaintenanceOrderStatus.PENDING) {
      throw new InvalidOrderStateException();
    }
    VehicleResponse vehicle = vehicleGateway.getVehicle(order.getVehicleId());
    if (BusinessConstants.VehicleStatus.AVAILABLE.equals(vehicle.status())) {
      vehicleGateway.changeStatus(
          order.getVehicleId(), BusinessConstants.VehicleStatus.IN_MAINTENANCE, null);
    } else if (!BusinessConstants.VehicleStatus.IN_MAINTENANCE.equals(vehicle.status())) {
      throw new VehicleNotAvailableException();
    }
    order.setStatus(MaintenanceOrderStatus.IN_PROGRESS);
    order.setStartedAt(Instant.now(clock));
    return orderMapper.toResponse(orderRepository.save(order));
  }

  /**
   * Completes an order in progress: frees the vehicle with its new odometer and recalculates the
   * plan from today and that odometer. The order and the plan are saved together.
   */
  public CompleteOrderResponse complete(UUID orderId, CompleteOrderRequest request) {
    MaintenanceOrder order = findOrder(orderId);
    if (order.getStatus() != MaintenanceOrderStatus.IN_PROGRESS) {
      throw new InvalidOrderStateException();
    }
    VehicleResponse vehicle = vehicleGateway.getVehicle(order.getVehicleId());
    int odometerKm = resolveOdometer(request.odometerKm(), vehicle);
    releaseVehicle(order, vehicle, odometerKm);

    Instant completedAt = Instant.now(clock);
    return transactionTemplate.execute(
        status -> persistCompletion(order, request, odometerKm, completedAt));
  }

  private CompleteOrderResponse persistCompletion(
      MaintenanceOrder order, CompleteOrderRequest request, int odometerKm, Instant completedAt) {
    MaintenancePlan plan =
        planRepository
            .findById(order.getPlanId())
            .orElseThrow(
                () -> new IllegalStateException("Plan not found for order " + order.getId()));
    plan.registerDone(LocalDate.ofInstant(completedAt, ZoneOffset.UTC), odometerKm);

    order.setStatus(MaintenanceOrderStatus.COMPLETED);
    order.setCompletedAt(completedAt);
    order.setOdometerKm(odometerKm);
    order.setCost(request.cost().setScale(BusinessConstants.MONEY_SCALE, RoundingMode.HALF_UP));
    order.setWorkshop(request.workshop());
    order.setNotes(request.notes());
    planRepository.save(plan);
    MaintenanceOrder saved = orderRepository.save(order);

    return new CompleteOrderResponse(
        saved.getId(),
        saved.getVehicleId(),
        saved.getType(),
        saved.getStatus(),
        saved.getCompletedAt(),
        saved.getCost(),
        saved.getOdometerKm(),
        saved.getWorkshop(),
        new CompleteOrderResponse.PlanSummary(
            plan.getId(), plan.getNextDueAt(), plan.getNextDueKm()));
  }

  private int resolveOdometer(Integer requested, VehicleResponse vehicle) {
    int current = vehicle.odometerKm() == null ? 0 : vehicle.odometerKm();
    if (requested == null) {
      return current;
    }
    if (requested < current) {
      throw new InvalidOdometerException();
    }
    return requested;
  }

  /**
   * Sets the vehicle back to available unless it is not in maintenance any more or another order of
   * the same vehicle is still in progress.
   */
  private void releaseVehicle(MaintenanceOrder order, VehicleResponse vehicle, int odometerKm) {
    boolean inMaintenance = BusinessConstants.VehicleStatus.IN_MAINTENANCE.equals(vehicle.status());
    boolean otherInProgress =
        orderRepository.countByVehicleIdAndStatus(
                order.getVehicleId(), MaintenanceOrderStatus.IN_PROGRESS)
            > 1;
    if (inMaintenance && !otherInProgress) {
      vehicleGateway.changeStatus(
          order.getVehicleId(), BusinessConstants.VehicleStatus.AVAILABLE, odometerKm);
    }
  }

  private MaintenanceOrder findOrder(UUID orderId) {
    return orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
  }
}
