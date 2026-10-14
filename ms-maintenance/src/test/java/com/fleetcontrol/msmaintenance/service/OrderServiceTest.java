package com.fleetcontrol.msmaintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msmaintenance.client.VehicleGateway;
import com.fleetcontrol.msmaintenance.exception.InvalidOdometerException;
import com.fleetcontrol.msmaintenance.exception.InvalidOrderStateException;
import com.fleetcontrol.msmaintenance.exception.OrderNotFoundException;
import com.fleetcontrol.msmaintenance.exception.ServiceUnavailableException;
import com.fleetcontrol.msmaintenance.exception.VehicleNotAvailableException;
import com.fleetcontrol.msmaintenance.mapper.MaintenanceOrderMapper;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.dto.request.CompleteOrderRequest;
import com.fleetcontrol.msmaintenance.models.dto.response.CompleteOrderResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenanceOrderRepository;
import com.fleetcontrol.msmaintenance.repository.MaintenancePlanRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

  @Mock private MaintenanceOrderRepository orderRepository;
  @Mock private MaintenancePlanRepository planRepository;
  @Mock private MaintenanceOrderMapper orderMapper;
  @Mock private VehicleGateway vehicleGateway;
  @Mock private TransactionTemplate transactionTemplate;

  private OrderService service;
  private final UUID orderId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private final UUID vehicleId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new OrderService(
            orderRepository,
            planRepository,
            orderMapper,
            vehicleGateway,
            transactionTemplate,
            CLOCK);
  }

  private MaintenanceOrder order(MaintenanceOrderStatus status) {
    MaintenanceOrder order = new MaintenanceOrder();
    order.setId(orderId);
    order.setPlanId(planId);
    order.setVehicleId(vehicleId);
    order.setType(MaintenanceType.OIL_CHANGE);
    order.setStatus(status);
    order.setScheduledFor(TODAY);
    return order;
  }

  private void givenOrder(MaintenanceOrderStatus status) {
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order(status)));
  }

  private void givenVehicle(String status, Integer odometerKm) {
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(new VehicleResponse(vehicleId, status, odometerKm));
  }

  private void givenSavesReturnTheirArgument() {
    when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private void givenTransactionRunsCallback() {
    when(transactionTemplate.execute(any()))
        .thenAnswer(
            invocation ->
                invocation.<TransactionCallback<Object>>getArgument(0).doInTransaction(null));
  }

  private void givenPlan() {
    MaintenancePlan plan = new MaintenancePlan();
    plan.setId(planId);
    plan.setVehicleId(vehicleId);
    plan.setType(MaintenanceType.OIL_CHANGE);
    plan.setIntervalKm(10_000);
    plan.setIntervalDays(90);
    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
  }

  // --- start ---

  @Test
  void start_pendingOrderWithAvailableVehicle_startsOrderAndPutsVehicleInMaintenance() {
    MaintenanceOrder order = order(MaintenanceOrderStatus.PENDING);
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    givenVehicle("AVAILABLE", 10_000);
    givenSavesReturnTheirArgument();

    service.start(orderId);

    verify(vehicleGateway).changeStatus(vehicleId, "IN_MAINTENANCE", null);
    assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.IN_PROGRESS);
    assertThat(order.getStartedAt()).isEqualTo(NOW);
  }

  @Test
  void start_vehicleAlreadyInMaintenance_startsWithoutChangingTheVehicle() {
    MaintenanceOrder order = order(MaintenanceOrderStatus.PENDING);
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    givenVehicle("IN_MAINTENANCE", 10_000);
    givenSavesReturnTheirArgument();

    service.start(orderId);

    verify(vehicleGateway, never()).changeStatus(any(), any(), any());
    assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.IN_PROGRESS);
  }

  @Test
  void start_vehicleInUse_throwsAndLeavesOrderPending() {
    MaintenanceOrder order = order(MaintenanceOrderStatus.PENDING);
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    givenVehicle("IN_USE", 10_000);

    assertThatThrownBy(() -> service.start(orderId))
        .isInstanceOf(VehicleNotAvailableException.class);
    assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.PENDING);
    verify(orderRepository, never()).save(any());
  }

  @Test
  void start_vehicleServiceDown_leavesOrderPending() {
    MaintenanceOrder order = order(MaintenanceOrderStatus.PENDING);
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    givenVehicle("AVAILABLE", 10_000);
    when(vehicleGateway.changeStatus(vehicleId, "IN_MAINTENANCE", null))
        .thenThrow(new ServiceUnavailableException("ms-vehicles"));

    assertThatThrownBy(() -> service.start(orderId))
        .isInstanceOf(ServiceUnavailableException.class);
    assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.PENDING);
    verify(orderRepository, never()).save(any());
  }

  @Test
  void start_orderNotPending_throwsWithoutCallingVehicles() {
    givenOrder(MaintenanceOrderStatus.IN_PROGRESS);

    assertThatThrownBy(() -> service.start(orderId)).isInstanceOf(InvalidOrderStateException.class);
    verifyNoInteractions(vehicleGateway);
  }

  @Test
  void start_unknownOrder_throwsNotFound() {
    when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.start(orderId)).isInstanceOf(OrderNotFoundException.class);
  }

  // --- complete ---

  @Test
  void complete_orderInProgress_freesVehicleAndRecalculatesPlan() {
    givenOrder(MaintenanceOrderStatus.IN_PROGRESS);
    givenVehicle("IN_MAINTENANCE", 50_000);
    when(orderRepository.countByVehicleIdAndStatus(vehicleId, MaintenanceOrderStatus.IN_PROGRESS))
        .thenReturn(1L);
    givenPlan();
    givenTransactionRunsCallback();
    givenSavesReturnTheirArgument();

    CompleteOrderResponse response =
        service.complete(
            orderId,
            new CompleteOrderRequest(new BigDecimal("80.5"), 50_100, "Taller Norte", "Todo ok"));

    verify(vehicleGateway).changeStatus(vehicleId, "AVAILABLE", 50_100);
    assertThat(response.status()).isEqualTo(MaintenanceOrderStatus.COMPLETED);
    assertThat(response.completedAt()).isEqualTo(NOW);
    assertThat(response.odometerKm()).isEqualTo(50_100);
    assertThat(response.cost()).isEqualByComparingTo("80.50");
    assertThat(response.cost().scale()).isEqualTo(2);
    assertThat(response.workshop()).isEqualTo("Taller Norte");
    assertThat(response.plan().id()).isEqualTo(planId);
    assertThat(response.plan().nextDueAt()).isEqualTo(TODAY.plusDays(90));
    assertThat(response.plan().nextDueKm()).isEqualTo(60_100);
    verify(planRepository).save(any(MaintenancePlan.class));
  }

  @Test
  void complete_withoutOdometer_usesTheOdometerOfTheVehicle() {
    givenOrder(MaintenanceOrderStatus.IN_PROGRESS);
    givenVehicle("IN_MAINTENANCE", 50_000);
    when(orderRepository.countByVehicleIdAndStatus(vehicleId, MaintenanceOrderStatus.IN_PROGRESS))
        .thenReturn(1L);
    givenPlan();
    givenTransactionRunsCallback();
    givenSavesReturnTheirArgument();

    CompleteOrderResponse response =
        service.complete(orderId, new CompleteOrderRequest(BigDecimal.TEN, null, null, null));

    assertThat(response.odometerKm()).isEqualTo(50_000);
    verify(vehicleGateway).changeStatus(vehicleId, "AVAILABLE", 50_000);
  }

  @Test
  void complete_odometerLowerThanCurrent_throwsAndChangesNothing() {
    givenOrder(MaintenanceOrderStatus.IN_PROGRESS);
    givenVehicle("IN_MAINTENANCE", 50_000);

    assertThatThrownBy(
            () ->
                service.complete(
                    orderId, new CompleteOrderRequest(BigDecimal.TEN, 49_999, null, null)))
        .isInstanceOf(InvalidOdometerException.class);
    verify(vehicleGateway, never()).changeStatus(any(), any(), any());
    verifyNoInteractions(transactionTemplate);
  }

  @Test
  void complete_anotherOrderOfTheVehicleInProgress_keepsVehicleInMaintenance() {
    givenOrder(MaintenanceOrderStatus.IN_PROGRESS);
    givenVehicle("IN_MAINTENANCE", 50_000);
    when(orderRepository.countByVehicleIdAndStatus(vehicleId, MaintenanceOrderStatus.IN_PROGRESS))
        .thenReturn(2L);
    givenPlan();
    givenTransactionRunsCallback();
    givenSavesReturnTheirArgument();

    CompleteOrderResponse response =
        service.complete(orderId, new CompleteOrderRequest(BigDecimal.TEN, null, null, null));

    verify(vehicleGateway, never()).changeStatus(any(), any(), any());
    assertThat(response.status()).isEqualTo(MaintenanceOrderStatus.COMPLETED);
  }

  @Test
  void complete_vehicleServiceDown_doesNotCompleteTheOrder() {
    MaintenanceOrder order = order(MaintenanceOrderStatus.IN_PROGRESS);
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    givenVehicle("IN_MAINTENANCE", 50_000);
    when(orderRepository.countByVehicleIdAndStatus(vehicleId, MaintenanceOrderStatus.IN_PROGRESS))
        .thenReturn(1L);
    when(vehicleGateway.changeStatus(vehicleId, "AVAILABLE", 50_000))
        .thenThrow(new ServiceUnavailableException("ms-vehicles"));

    assertThatThrownBy(
            () ->
                service.complete(
                    orderId, new CompleteOrderRequest(BigDecimal.TEN, null, null, null)))
        .isInstanceOf(ServiceUnavailableException.class);
    assertThat(order.getStatus()).isEqualTo(MaintenanceOrderStatus.IN_PROGRESS);
    verifyNoInteractions(transactionTemplate);
  }

  @Test
  void complete_orderNotInProgress_throws() {
    givenOrder(MaintenanceOrderStatus.PENDING);

    assertThatThrownBy(
            () ->
                service.complete(
                    orderId, new CompleteOrderRequest(BigDecimal.TEN, null, null, null)))
        .isInstanceOf(InvalidOrderStateException.class);
    verifyNoInteractions(vehicleGateway);
  }
}
