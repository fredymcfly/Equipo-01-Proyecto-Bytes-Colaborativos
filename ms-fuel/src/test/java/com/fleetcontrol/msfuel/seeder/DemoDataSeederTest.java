package com.fleetcontrol.msfuel.seeder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msfuel.client.VehicleClient;
import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.config.DemoProperties;
import com.fleetcontrol.msfuel.exception.DependencyUnavailableException;
import com.fleetcontrol.msfuel.exception.ErrorCode;
import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.repository.FuelRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of how the demo seeder reads the vehicles and stores the refuels. */
@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

  private static final Instant NOW = Instant.parse("2026-10-05T10:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private FuelRecordRepository repository;
  @Mock private VehicleClient vehicleClient;
  @Captor private ArgumentCaptor<List<FuelRecord>> saved;

  @Test
  void trySeed_withoutRefuels_readsTheVehiclesByTheirDemoIdsAndSavesTheRefuels() {
    stubVehicles();

    boolean done = seeder(new DemoProperties(42, 90)).trySeed();

    assertThat(done).isTrue();
    verify(repository).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(300);
    for (int number = 1; number <= DemoConstants.VEHICLE_COUNT; number++) {
      verify(vehicleClient).getVehicle(DemoConstants.vehicleId(number));
    }
  }

  @Test
  void trySeed_withoutProperties_usesTheDefaults() {
    stubVehicles();

    assertThat(seeder(new DemoProperties(null, null)).trySeed()).isTrue();

    verify(repository).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(300);
  }

  @Test
  void trySeed_refuelsAlreadyPresent_doesNothing() {
    when(repository.count()).thenReturn(10L);

    assertThat(seeder(new DemoProperties(42, 90)).trySeed()).isTrue();

    verify(repository, never()).saveAll(anyList());
    verifyNoInteractions(vehicleClient);
  }

  @Test
  void trySeed_vehiclesNotReady_asksToTryAgainWithoutSaving() {
    when(repository.count()).thenReturn(0L);
    when(vehicleClient.getVehicle(any()))
        .thenThrow(
            new DependencyUnavailableException("ms-vehicles", ErrorCode.SERVICE_UNAVAILABLE));

    assertThat(seeder(new DemoProperties(42, 90)).trySeed()).isFalse();

    verify(repository, never()).saveAll(anyList());
  }

  private DemoDataSeeder seeder(DemoProperties properties) {
    return new DemoDataSeeder(repository, vehicleClient, properties, CLOCK);
  }

  private void stubVehicles() {
    when(repository.count()).thenReturn(0L);
    when(vehicleClient.getVehicle(any()))
        .thenAnswer(
            inv ->
                new VehicleResponse(
                    inv.getArgument(0),
                    "0000-BBB",
                    "Ford",
                    "Transit",
                    2021,
                    "VAN",
                    "DIESEL",
                    80,
                    50_000,
                    "AVAILABLE",
                    NOW,
                    NOW));
  }
}
