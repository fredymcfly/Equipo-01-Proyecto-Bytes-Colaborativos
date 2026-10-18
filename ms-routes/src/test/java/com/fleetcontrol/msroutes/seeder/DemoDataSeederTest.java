package com.fleetcontrol.msroutes.seeder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msroutes.client.DriverClient;
import com.fleetcontrol.msroutes.client.VehicleClient;
import com.fleetcontrol.msroutes.client.dto.DriverResponse;
import com.fleetcontrol.msroutes.client.dto.VehicleResponse;
import com.fleetcontrol.msroutes.client.enums.DriverStatus;
import com.fleetcontrol.msroutes.client.enums.LicenseCategory;
import com.fleetcontrol.msroutes.client.enums.VehicleStatus;
import com.fleetcontrol.msroutes.client.enums.VehicleType;
import com.fleetcontrol.msroutes.config.DemoProperties;
import com.fleetcontrol.msroutes.constants.DemoConstants;
import com.fleetcontrol.msroutes.entities.Route;
import com.fleetcontrol.msroutes.repository.RouteRepository;
import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of how the demo seeder reads the fleet and stores the routes. */
@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-05T10:30:00Z"), ZoneOffset.UTC);

  @Mock private RouteRepository repository;
  @Mock private VehicleClient vehicleClient;
  @Mock private DriverClient driverClient;
  @Captor private ArgumentCaptor<List<Route>> saved;

  @Test
  void trySeed_withoutRoutes_readsTheFleetByItsDemoIdsAndSavesTheRoutes() {
    stubFleet();
    DemoDataSeeder seeder = seeder(new DemoProperties(42, 30));

    boolean done = seeder.trySeed();

    assertThat(done).isTrue();
    verify(repository).saveAll(saved.capture());
    assertThat(saved.getValue()).isNotEmpty();
    assertThat(saved.getValue())
        .allSatisfy(
            route -> {
              assertThat(route.getCreatedBy()).isEqualTo(DemoConstants.adminId(42));
              assertThat(route.getPlannedStart()).isAfter(Instant.parse("2026-09-04T00:00:00Z"));
            });
  }

  @Test
  void trySeed_withoutProperties_usesTheDefaults() {
    stubFleet();
    DemoDataSeeder seeder = seeder(new DemoProperties(null, null));

    assertThat(seeder.trySeed()).isTrue();

    verify(repository).saveAll(saved.capture());
    assertThat(saved.getValue().get(0).getCreatedBy())
        .isEqualTo(DemoConstants.adminId(DemoConstants.DEFAULT_SEED));
  }

  @Test
  void trySeed_routesAlreadyPresent_doesNothing() {
    when(repository.count()).thenReturn(5L);

    assertThat(seeder(new DemoProperties(42, 30)).trySeed()).isTrue();

    verify(repository, never()).saveAll(anyList());
    verifyNoInteractions(vehicleClient, driverClient);
  }

  @Test
  void trySeed_vehiclesNotReady_asksToTryAgainWithoutSaving() {
    when(repository.count()).thenReturn(0L);
    when(vehicleClient.getVehicle(any())).thenThrow(feignError());

    assertThat(seeder(new DemoProperties(42, 30)).trySeed()).isFalse();

    verify(repository, never()).saveAll(anyList());
  }

  @Test
  void trySeed_driversNotReady_asksToTryAgainWithoutSaving() {
    when(repository.count()).thenReturn(0L);
    when(vehicleClient.getVehicle(any()))
        .thenAnswer(
            inv ->
                new VehicleResponse(
                    inv.getArgument(0), VehicleType.VAN, VehicleStatus.AVAILABLE, 40_000));
    when(driverClient.getDriver(any())).thenThrow(feignError());

    assertThat(seeder(new DemoProperties(42, 30)).trySeed()).isFalse();

    verify(repository, never()).saveAll(anyList());
  }

  private DemoDataSeeder seeder(DemoProperties properties) {
    return new DemoDataSeeder(repository, vehicleClient, driverClient, properties, CLOCK);
  }

  private void stubFleet() {
    when(repository.count()).thenReturn(0L);
    when(vehicleClient.getVehicle(any()))
        .thenAnswer(
            inv ->
                new VehicleResponse(
                    inv.getArgument(0), VehicleType.VAN, VehicleStatus.AVAILABLE, 40_000));
    when(driverClient.getDriver(any()))
        .thenAnswer(
            inv ->
                new DriverResponse(
                    inv.getArgument(0),
                    LicenseCategory.C,
                    LocalDate.of(2030, 1, 1),
                    DriverStatus.ACTIVE));
  }

  private static FeignException feignError() {
    Request request =
        Request.create(
            Request.HttpMethod.GET,
            "http://downstream/api",
            Map.of(),
            (byte[]) null,
            StandardCharsets.UTF_8,
            null);
    return new FeignException.ServiceUnavailable("unavailable", request, new byte[0], Map.of());
  }
}
