package com.fleetcontrol.msmaintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msmaintenance.client.VehicleGateway;
import com.fleetcontrol.msmaintenance.exception.PlanAlreadyExistsException;
import com.fleetcontrol.msmaintenance.exception.VehicleNotFoundException;
import com.fleetcontrol.msmaintenance.mapper.MaintenancePlanMapper;
import com.fleetcontrol.msmaintenance.models.dto.internal.VehicleResponse;
import com.fleetcontrol.msmaintenance.models.dto.request.CreatePlanRequest;
import com.fleetcontrol.msmaintenance.models.dto.response.PageResponse;
import com.fleetcontrol.msmaintenance.models.dto.response.PlanResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenancePlanRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

  @Mock private MaintenancePlanRepository repository;
  @Mock private MaintenancePlanMapper mapper;
  @Mock private VehicleGateway vehicleGateway;

  private PlanService service;
  private final UUID vehicleId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new PlanService(repository, mapper, vehicleGateway, CLOCK);
  }

  private void vehicleWithOdometer(Integer odometerKm) {
    when(vehicleGateway.getVehicle(vehicleId))
        .thenReturn(new VehicleResponse(vehicleId, "AVAILABLE", odometerKm));
  }

  private MaintenancePlan savedPlan() {
    ArgumentCaptor<MaintenancePlan> captor = ArgumentCaptor.forClass(MaintenancePlan.class);
    verify(repository).saveAndFlush(captor.capture());
    return captor.getValue();
  }

  @Test
  void create_withoutLastIntervention_usesTodayAndVehicleOdometer() {
    vehicleWithOdometer(42_000);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.create(
        new CreatePlanRequest(vehicleId, MaintenanceType.OIL_CHANGE, 10_000, 180, null, null));

    MaintenancePlan plan = savedPlan();
    assertThat(plan.getLastDoneAt()).isEqualTo(TODAY);
    assertThat(plan.getLastDoneKm()).isEqualTo(42_000);
    assertThat(plan.getNextDueAt()).isEqualTo(TODAY.plusDays(180));
    assertThat(plan.getNextDueKm()).isEqualTo(52_000);
  }

  @Test
  void create_withLastIntervention_usesGivenValues() {
    vehicleWithOdometer(42_000);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    LocalDate lastDoneAt = LocalDate.of(2026, 6, 1);

    service.create(
        new CreatePlanRequest(
            vehicleId, MaintenanceType.BRAKE_CHECK, 20_000, null, lastDoneAt, 30_000));

    MaintenancePlan plan = savedPlan();
    assertThat(plan.getLastDoneAt()).isEqualTo(lastDoneAt);
    assertThat(plan.getLastDoneKm()).isEqualTo(30_000);
    assertThat(plan.getNextDueKm()).isEqualTo(50_000);
    assertThat(plan.getNextDueAt()).isNull();
  }

  @Test
  void create_vehicleWithoutOdometer_startsFromZero() {
    vehicleWithOdometer(null);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.create(
        new CreatePlanRequest(vehicleId, MaintenanceType.OIL_CHANGE, 10_000, null, null, null));

    assertThat(savedPlan().getLastDoneKm()).isZero();
  }

  @Test
  void create_vehicleAlreadyHasActivePlanOfThatType_throwsConflict() {
    vehicleWithOdometer(1_000);
    when(repository.existsByVehicleIdAndTypeAndActiveTrue(vehicleId, MaintenanceType.OIL_CHANGE))
        .thenReturn(true);
    CreatePlanRequest request =
        new CreatePlanRequest(vehicleId, MaintenanceType.OIL_CHANGE, 10_000, null, null, null);

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(PlanAlreadyExistsException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void create_duplicateDetectedByDatabase_throwsConflict() {
    vehicleWithOdometer(1_000);
    when(repository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("uq_plans_active_vehicle_type"));
    CreatePlanRequest request =
        new CreatePlanRequest(vehicleId, MaintenanceType.OIL_CHANGE, 10_000, null, null, null);

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(PlanAlreadyExistsException.class);
  }

  @Test
  void create_unknownVehicle_propagatesErrorWithoutTouchingDatabase() {
    when(vehicleGateway.getVehicle(vehicleId)).thenThrow(new VehicleNotFoundException());
    CreatePlanRequest request =
        new CreatePlanRequest(vehicleId, MaintenanceType.OIL_CHANGE, 10_000, null, null, null);

    assertThatThrownBy(() -> service.create(request)).isInstanceOf(VehicleNotFoundException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void list_returnsPageEnvelopeSortedByDueDate() {
    MaintenancePlan plan = new MaintenancePlan();
    PlanResponse response =
        new PlanResponse(
            UUID.randomUUID(),
            vehicleId,
            MaintenanceType.OIL_CHANGE,
            10_000,
            null,
            TODAY,
            1_000,
            null,
            11_000,
            true);
    when(repository.findAll(
            ArgumentMatchers.<Specification<MaintenancePlan>>any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(plan), PageRequest.of(1, 5), 11));
    when(mapper.toResponse(plan)).thenReturn(response);

    PageResponse<PlanResponse> page = service.list(vehicleId, null, true, 1, 5);

    assertThat(page.content()).containsExactly(response);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(5);
    assertThat(page.totalElements()).isEqualTo(11);
    assertThat(page.totalPages()).isEqualTo(3);

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository)
        .findAll(ArgumentMatchers.<Specification<MaintenancePlan>>any(), captor.capture());
    assertThat(captor.getValue().getSort().getOrderFor("nextDueAt")).isNotNull();
    assertThat(captor.getValue().getSort().getOrderFor("nextDueAt").isAscending()).isTrue();
  }
}
