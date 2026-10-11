package com.fleetcontrol.msvehicles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msvehicles.dto.CreateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.UpdateVehicleRequest;
import com.fleetcontrol.msvehicles.dto.VehicleResponse;
import com.fleetcontrol.msvehicles.exception.VehicleAlreadyExistsException;
import com.fleetcontrol.msvehicles.exception.VehicleNotFoundException;
import com.fleetcontrol.msvehicles.mapper.VehicleMapper;
import com.fleetcontrol.msvehicles.model.FuelType;
import com.fleetcontrol.msvehicles.model.Vehicle;
import com.fleetcontrol.msvehicles.model.VehicleStatus;
import com.fleetcontrol.msvehicles.model.VehicleType;
import com.fleetcontrol.msvehicles.repository.VehicleRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  private static CreateVehicleRequest createRequest() {
    return new CreateVehicleRequest(
        PLATE, MAKE, MODEL, 2022, VehicleType.VAN, FuelType.DIESEL, 80, 45210);
  }

  private static UpdateVehicleRequest updateRequest() {
    return new UpdateVehicleRequest(MAKE, MODEL, 2023, VehicleType.VAN, FuelType.DIESEL, 85);
  }
}
