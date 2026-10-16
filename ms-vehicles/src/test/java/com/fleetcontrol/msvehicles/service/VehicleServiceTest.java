package com.fleetcontrol.msvehicles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msvehicles.exception.ForbiddenException;
import com.fleetcontrol.msvehicles.exception.InvalidOdometerException;
import com.fleetcontrol.msvehicles.exception.InvalidStatusTransitionException;
import com.fleetcontrol.msvehicles.exception.VehicleAlreadyExistsException;
import com.fleetcontrol.msvehicles.exception.VehicleNotFoundException;
import com.fleetcontrol.msvehicles.mapper.VehicleMapper;
import com.fleetcontrol.msvehicles.models.dto.request.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.models.dto.request.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.models.dto.request.VehicleStatusRequest;
import com.fleetcontrol.msvehicles.models.dto.response.FleetSummaryResponse;
import com.fleetcontrol.msvehicles.models.dto.response.VehicleResponse;
import com.fleetcontrol.msvehicles.models.entities.Vehicle;
import com.fleetcontrol.msvehicles.models.enums.FuelType;
import com.fleetcontrol.msvehicles.models.enums.VehicleStatus;
import com.fleetcontrol.msvehicles.models.enums.VehicleType;
import com.fleetcontrol.msvehicles.repository.VehicleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for the vehicle business rules. */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

  private static final String PLATE = "4821-KDF";
  private static final String MAKE = "Ford";
  private static final String MODEL = "Transit";

  @Mock private VehicleRepository repository;
  @Mock private VehicleMapper mapper;
  @InjectMocks private VehicleService service;

  @Test
  void createRejectsDuplicatedPlate() {
    when(repository.existsByPlate(PLATE)).thenReturn(true);

    assertThatThrownBy(() -> service.create(createRequest()))
        .isInstanceOf(VehicleAlreadyExistsException.class);
  }

  @Test
  void createStoresVehicleAsAvailable() {
    Vehicle vehicle = new Vehicle();
    when(repository.existsByPlate(anyString())).thenReturn(false);
    when(mapper.toEntity(any(CreateVehicleRequest.class))).thenReturn(vehicle);
    when(repository.save(vehicle)).thenReturn(vehicle);
    when(mapper.toResponse(vehicle)).thenReturn(null);

    service.create(createRequest());

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    verify(repository).save(vehicle);
  }

  @Test
  void getByIdFailsWhenVehicleDoesNotExist() {
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(vehicleId))
        .isInstanceOf(VehicleNotFoundException.class);
  }

  @Test
  void getByIdReturnsMappedVehicle() {
    UUID vehicleId = UUID.randomUUID();
    Vehicle vehicle = new Vehicle();
    VehicleResponse response =
        new VehicleResponse(
            vehicleId,
            PLATE,
            MAKE,
            MODEL,
            2022,
            VehicleType.VAN,
            FuelType.DIESEL,
            80,
            45210,
            VehicleStatus.AVAILABLE,
            null,
            null);
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    when(mapper.toResponse(vehicle)).thenReturn(response);

    assertThat(service.getById(vehicleId)).isSameAs(response);
  }

  @Test
  void updateFailsWhenVehicleDoesNotExist() {
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(vehicleId, updateRequest()))
        .isInstanceOf(VehicleNotFoundException.class);
  }

  @Test
  void updateKeepsPlateStatusAndOdometer() {
    Vehicle vehicle = new Vehicle();
    vehicle.setPlate(PLATE);
    vehicle.setStatus(VehicleStatus.IN_USE);
    vehicle.setOdometerKm(45210);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    when(repository.save(vehicle)).thenReturn(vehicle);
    when(mapper.toResponse(vehicle)).thenReturn(null);
    UpdateVehicleRequest request = updateRequest();

    service.update(vehicleId, request);

    verify(mapper).applyUpdate(request, vehicle);
    assertThat(vehicle.getPlate()).isEqualTo(PLATE);
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);
    assertThat(vehicle.getOdometerKm()).isEqualTo(45210);
  }

  @Test
  void changeStatusAppliesAllowedTransition() {
    Vehicle vehicle = new Vehicle();
    vehicle.setStatus(VehicleStatus.AVAILABLE);
    vehicle.setOdometerKm(45210);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    when(repository.save(vehicle)).thenReturn(vehicle);
    when(mapper.toResponse(vehicle)).thenReturn(null);

    service.changeStatus(vehicleId, new VehicleStatusRequest(VehicleStatus.IN_USE, null));

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);
  }

  @Test
  void changeStatusRejectsSameStatus() {
    Vehicle vehicle = new Vehicle();
    vehicle.setStatus(VehicleStatus.AVAILABLE);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

    assertThatThrownBy(
            () ->
                service.changeStatus(
                    vehicleId, new VehicleStatusRequest(VehicleStatus.AVAILABLE, null)))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  @Test
  void changeStatusRejectsDisallowedTransition() {
    Vehicle vehicle = new Vehicle();
    vehicle.setStatus(VehicleStatus.IN_USE);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

    assertThatThrownBy(
            () ->
                service.changeStatus(
                    vehicleId, new VehicleStatusRequest(VehicleStatus.IN_MAINTENANCE, null)))
        .isInstanceOf(InvalidStatusTransitionException.class);
  }

  @Test
  void changeStatusRejectsLowerOdometer() {
    Vehicle vehicle = new Vehicle();
    vehicle.setStatus(VehicleStatus.AVAILABLE);
    vehicle.setOdometerKm(45210);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

    assertThatThrownBy(
            () ->
                service.changeStatus(
                    vehicleId, new VehicleStatusRequest(VehicleStatus.IN_USE, 45000)))
        .isInstanceOf(InvalidOdometerException.class);
  }

  @Test
  void changeStatusUpdatesOdometerWhenHigher() {
    Vehicle vehicle = new Vehicle();
    vehicle.setStatus(VehicleStatus.AVAILABLE);
    vehicle.setOdometerKm(45210);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    when(repository.save(vehicle)).thenReturn(vehicle);
    when(mapper.toResponse(vehicle)).thenReturn(null);

    service.changeStatus(vehicleId, new VehicleStatusRequest(VehicleStatus.IN_USE, 45310));

    assertThat(vehicle.getOdometerKm()).isEqualTo(45310);
  }

  @Test
  void changeStatusRequiresAdminToReactivate() {
    Vehicle vehicle = new Vehicle();
    vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

    assertThatThrownBy(
            () ->
                service.changeStatus(
                    vehicleId, new VehicleStatusRequest(VehicleStatus.AVAILABLE, null)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void changeStatusAllowsAdminToReactivate() {
    setRole("ROLE_ADMIN");
    Vehicle vehicle = new Vehicle();
    vehicle.setStatus(VehicleStatus.OUT_OF_SERVICE);
    UUID vehicleId = UUID.randomUUID();
    when(repository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    when(repository.save(vehicle)).thenReturn(vehicle);
    when(mapper.toResponse(vehicle)).thenReturn(null);

    service.changeStatus(vehicleId, new VehicleStatusRequest(VehicleStatus.AVAILABLE, null));

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
  }

  @Test
  void summaryCountsEveryStatusAndTypeEvenWhenEmpty() {
    List<VehicleRepository.StatusCount> statusCounts =
        List.of(statusCount(VehicleStatus.AVAILABLE, 14), statusCount(VehicleStatus.IN_USE, 8));
    List<VehicleRepository.TypeCount> typeCounts =
        List.of(typeCount(VehicleType.CAR, 9), typeCount(VehicleType.VAN, 13));
    when(repository.countGroupedByStatus()).thenReturn(statusCounts);
    when(repository.countGroupedByType()).thenReturn(typeCounts);

    FleetSummaryResponse summary = service.summary();

    assertThat(summary.total()).isEqualTo(22);
    assertThat(summary.byStatus())
        .containsEntry(VehicleStatus.AVAILABLE, 14L)
        .containsEntry(VehicleStatus.IN_USE, 8L)
        .containsEntry(VehicleStatus.IN_MAINTENANCE, 0L)
        .containsEntry(VehicleStatus.OUT_OF_SERVICE, 0L);
    assertThat(summary.byType())
        .containsEntry(VehicleType.CAR, 9L)
        .containsEntry(VehicleType.VAN, 13L)
        .containsEntry(VehicleType.TRUCK, 0L)
        .containsEntry(VehicleType.MOTORCYCLE, 0L);
  }

  @Test
  void summaryOfAnEmptyFleetIsAllZeros() {
    when(repository.countGroupedByStatus()).thenReturn(List.of());
    when(repository.countGroupedByType()).thenReturn(List.of());

    FleetSummaryResponse summary = service.summary();

    assertThat(summary.total()).isZero();
    assertThat(summary.byStatus()).hasSize(VehicleStatus.values().length).containsValue(0L);
    assertThat(summary.byType()).hasSize(VehicleType.values().length).containsValue(0L);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static VehicleRepository.StatusCount statusCount(VehicleStatus status, long total) {
    VehicleRepository.StatusCount count = mock(VehicleRepository.StatusCount.class);
    when(count.getVehicleStatus()).thenReturn(status);
    when(count.getTotal()).thenReturn(total);
    return count;
  }

  private static VehicleRepository.TypeCount typeCount(VehicleType type, long total) {
    VehicleRepository.TypeCount count = mock(VehicleRepository.TypeCount.class);
    when(count.getVehicleType()).thenReturn(type);
    when(count.getTotal()).thenReturn(total);
    return count;
  }

  private static void setRole(String authority) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "tester", null, List.of(new SimpleGrantedAuthority(authority)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private static CreateVehicleRequest createRequest() {
    return new CreateVehicleRequest(
        PLATE, MAKE, MODEL, 2022, VehicleType.VAN, FuelType.DIESEL, 80, 45210);
  }

  private static UpdateVehicleRequest updateRequest() {
    return new UpdateVehicleRequest(MAKE, MODEL, 2023, VehicleType.VAN, FuelType.DIESEL, 85);
  }
}
