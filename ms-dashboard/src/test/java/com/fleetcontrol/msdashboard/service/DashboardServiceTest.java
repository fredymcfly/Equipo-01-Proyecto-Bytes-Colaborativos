package com.fleetcontrol.msdashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msdashboard.client.AlertsClient;
import com.fleetcontrol.msdashboard.client.FuelClient;
import com.fleetcontrol.msdashboard.client.MaintenanceClient;
import com.fleetcontrol.msdashboard.client.RoutesClient;
import com.fleetcontrol.msdashboard.client.VehiclesClient;
import com.fleetcontrol.msdashboard.models.dto.internal.AlertsStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FleetSummaryDto;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceOrdersDto;
import com.fleetcontrol.msdashboard.models.dto.request.DashboardQuery;
import com.fleetcontrol.msdashboard.models.dto.response.DashboardResponse;
import com.fleetcontrol.msdashboard.models.dto.response.UpcomingMaintenanceResponse;
import com.fleetcontrol.msdashboard.support.FeignFailures;
import com.fleetcontrol.msdashboard.support.SourceData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-05T10:30:00Z");
  private static final String FROM = "2026-09-05";
  private static final String TO = "2026-10-05";
  private static final String MONTH = "MONTH";
  private static final String PENDING = "PENDING";
  private static final DashboardQuery THIRTY_DAYS = new DashboardQuery(30);

  @Mock private VehiclesClient vehiclesClient;
  @Mock private RoutesClient routesClient;
  @Mock private FuelClient fuelClient;
  @Mock private MaintenanceClient maintenanceClient;
  @Mock private AlertsClient alertsClient;

  private DashboardService service;

  @BeforeEach
  void setUp() {
    service =
        new DashboardService(
            vehiclesClient,
            routesClient,
            fuelClient,
            maintenanceClient,
            alertsClient,
            new SourceCaller(Runnable::run),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private void stubEverySource() {
    when(vehiclesClient.summary()).thenReturn(SourceData.fleet());
    when(routesClient.stats(FROM, TO, MONTH)).thenReturn(SourceData.routesStats());
    when(fuelClient.stats(FROM, TO, MONTH)).thenReturn(SourceData.fuelStats());
    when(maintenanceClient.stats(FROM, TO, MONTH)).thenReturn(SourceData.maintenanceStats());
    when(maintenanceClient.orders(PENDING, 0, 5)).thenReturn(SourceData.orders());
    when(alertsClient.stats(FROM, TO, MONTH)).thenReturn(SourceData.alertsStats());
  }

  @Test
  void overview_usesTheLastDaysUpToToday() {
    stubEverySource();
    when(vehiclesClient.getVehicle(SourceData.VAN_ID))
        .thenReturn(SourceData.vehicle(SourceData.VAN_ID, SourceData.VAN_PLATE));

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.generatedAt()).isEqualTo(NOW);
    assertThat(response.period().from()).isEqualTo(LocalDate.parse(FROM));
    assertThat(response.period().to()).isEqualTo(LocalDate.parse(TO));
    verify(routesClient).stats(FROM, TO, MONTH);
    verify(maintenanceClient).orders(PENDING, 0, 5);
  }

  @Test
  void overview_mapsEverySourceIntoItsBlock() {
    stubEverySource();
    when(vehiclesClient.getVehicle(SourceData.VAN_ID))
        .thenReturn(SourceData.vehicle(SourceData.VAN_ID, SourceData.VAN_PLATE));

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.partial()).isFalse();
    assertThat(response.unavailable()).isEmpty();
    assertThat(response.fleet().total()).isEqualTo(25);
    assertThat(response.fleet().available()).isEqualTo(14);
    assertThat(response.fleet().inUse()).isEqualTo(8);
    assertThat(response.fleet().inMaintenance()).isEqualTo(2);
    assertThat(response.fleet().outOfService()).isEqualTo(1);
    assertThat(response.kpis().routesCompleted()).isEqualTo(412);
    assertThat(response.kpis().distanceKm()).isEqualByComparingTo("38420.5");
    assertThat(response.kpis().fuelLiters()).isEqualByComparingTo("4226.3");
    assertThat(response.kpis().fuelCost()).isEqualByComparingTo("6846.61");
    assertThat(response.kpis().avgConsumptionL100km()).isEqualByComparingTo("11.0");
    assertThat(response.kpis().maintenanceCost()).isEqualByComparingTo("2140.25");
    assertThat(response.alerts().open()).isEqualTo(11);
    assertThat(response.alerts().critical()).isEqualTo(2);
    assertThat(response.alerts().warning()).isEqualTo(9);
    assertThat(response.alerts().info()).isZero();
    assertThat(response.upcomingMaintenance()).hasSize(1);
    assertThat(response.upcomingMaintenance().get(0).plate()).isEqualTo(SourceData.VAN_PLATE);
    assertThat(response.upcomingMaintenance().get(0).orderId()).isEqualTo(SourceData.ORDER_ID);
  }

  @Test
  void overview_countsMissingStatusesAndSeveritiesAsZero() {
    stubEverySource();
    when(vehiclesClient.summary()).thenReturn(new FleetSummaryDto(3, Map.of("IN_USE", 3L), null));
    when(alertsClient.stats(FROM, TO, MONTH))
        .thenReturn(new AlertsStatsDto(new AlertsStatsDto.Totals(0), List.of(), null));
    when(vehiclesClient.getVehicle(SourceData.VAN_ID))
        .thenReturn(SourceData.vehicle(SourceData.VAN_ID, SourceData.VAN_PLATE));

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.fleet().total()).isEqualTo(3);
    assertThat(response.fleet().inUse()).isEqualTo(3);
    assertThat(response.fleet().available()).isZero();
    assertThat(response.fleet().outOfService()).isZero();
    assertThat(response.alerts().open()).isZero();
    assertThat(response.alerts().critical()).isZero();
  }

  @Test
  void overview_looksUpEachPlateOnce() {
    UUID other = SourceData.CAR_ID;
    stubEverySource();
    when(maintenanceClient.orders(PENDING, 0, 5))
        .thenReturn(
            new MaintenanceOrdersDto(
                List.of(
                    new MaintenanceOrdersDto.Order(
                        UUID.randomUUID(), SourceData.VAN_ID, "OIL_CHANGE", LocalDate.parse(TO)),
                    new MaintenanceOrdersDto.Order(
                        UUID.randomUUID(), SourceData.VAN_ID, "BRAKE_CHECK", LocalDate.parse(TO)),
                    new MaintenanceOrdersDto.Order(
                        UUID.randomUUID(), other, "TIRE_ROTATION", LocalDate.parse(TO)))));
    when(vehiclesClient.getVehicle(SourceData.VAN_ID))
        .thenReturn(SourceData.vehicle(SourceData.VAN_ID, SourceData.VAN_PLATE));
    when(vehiclesClient.getVehicle(other))
        .thenReturn(SourceData.vehicle(other, SourceData.CAR_PLATE));

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.upcomingMaintenance())
        .extracting(UpcomingMaintenanceResponse::plate)
        .containsExactly(SourceData.VAN_PLATE, SourceData.VAN_PLATE, SourceData.CAR_PLATE);
    verify(vehiclesClient, times(1)).getVehicle(SourceData.VAN_ID);
    verify(vehiclesClient, times(1)).getVehicle(other);
  }

  @Test
  void overview_whenPlateCannotBeRead_keepsTheOrderWithoutPlate() {
    stubEverySource();
    when(vehiclesClient.getVehicle(SourceData.VAN_ID)).thenThrow(FeignFailures.unavailable());

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.partial()).isFalse();
    assertThat(response.upcomingMaintenance()).hasSize(1);
    assertThat(response.upcomingMaintenance().get(0).plate()).isNull();
  }

  @Test
  void overview_whenServiceFails_isPartialAndOnlyItsBlockIsNull() {
    stubEverySource();
    when(alertsClient.stats(FROM, TO, MONTH)).thenThrow(FeignFailures.unavailable());
    when(vehiclesClient.getVehicle(SourceData.VAN_ID))
        .thenReturn(SourceData.vehicle(SourceData.VAN_ID, SourceData.VAN_PLATE));

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.partial()).isTrue();
    assertThat(response.unavailable()).containsExactly("ms-alerts");
    assertThat(response.alerts()).isNull();
    assertThat(response.fleet()).isNotNull();
    assertThat(response.kpis().routesCompleted()).isEqualTo(412);
    assertThat(response.upcomingMaintenance()).hasSize(1);
  }

  @Test
  void overview_whenVehiclesFail_doesNotLookUpPlates() {
    stubEverySource();
    when(vehiclesClient.summary()).thenThrow(FeignFailures.unavailable());

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.unavailable()).containsExactly("ms-vehicles");
    assertThat(response.fleet()).isNull();
    assertThat(response.upcomingMaintenance().get(0).plate()).isNull();
    verify(vehiclesClient, never()).getVehicle(SourceData.VAN_ID);
  }

  @Test
  void overview_reportsMaintenanceOnceWhenBothOfItsCallsFail() {
    stubEverySource();
    when(maintenanceClient.stats(FROM, TO, MONTH)).thenThrow(FeignFailures.unavailable());
    when(maintenanceClient.orders(PENDING, 0, 5)).thenThrow(FeignFailures.unavailable());

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.unavailable()).containsExactly("ms-maintenance");
    assertThat(response.kpis().maintenanceCost()).isNull();
    assertThat(response.kpis().fuelCost()).isEqualByComparingTo(new BigDecimal("6846.61"));
    assertThat(response.upcomingMaintenance()).isNull();
  }

  @Test
  void overview_listsUnavailableServicesInTheOrderOfTheDocumentation() {
    stubEverySource();
    when(vehiclesClient.summary()).thenThrow(FeignFailures.unavailable());
    when(routesClient.stats(FROM, TO, MONTH)).thenThrow(FeignFailures.unavailable());
    when(fuelClient.stats(FROM, TO, MONTH)).thenThrow(FeignFailures.unavailable());
    when(alertsClient.stats(FROM, TO, MONTH)).thenThrow(FeignFailures.unavailable());

    DashboardResponse response = service.overview(THIRTY_DAYS);

    assertThat(response.partial()).isTrue();
    assertThat(response.unavailable())
        .containsExactly("ms-vehicles", "ms-routes", "ms-fuel", "ms-alerts");
    assertThat(response.kpis().maintenanceCost()).isNotNull();
  }
}
