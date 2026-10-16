package com.fleetcontrol.msfuel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fleetcontrol.msfuel.client.VehicleClient;
import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.dto.FuelConsumptionResponse;
import com.fleetcontrol.msfuel.dto.FuelRecordRequest;
import com.fleetcontrol.msfuel.dto.FuelRecordResponse;
import com.fleetcontrol.msfuel.dto.FuelStatsResponse;
import com.fleetcontrol.msfuel.dto.Granularity;
import com.fleetcontrol.msfuel.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recorre el flujo completo del servicio contra un PostgreSQL real: registra repostajes, calcula el
 * consumo entre depósitos llenos y comprueba los agregados de consumo y estadísticas.
 */
@Transactional
class FuelServiceIntegrationTest extends PostgresIntegrationTest {

  private static final UUID VEHICLE = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final LocalDate FROM = LocalDate.parse("2026-09-01");
  private static final LocalDate TO = LocalDate.parse("2026-09-30");

  private static final VehicleResponse VEHICLE_RESPONSE =
      new VehicleResponse(
          VEHICLE, "1234ABC", "Ford", "Transit", 2021, "VAN", "DIESEL", 80, 900, "ACTIVE", null,
          null);

  @Autowired private FuelService service;

  @MockitoBean private VehicleClient vehicleClient;

  @Test
  void registersRefuelsAndReportsConsumptionAndStatsAgainstRealPostgres() {
    given(vehicleClient.getVehicle(VEHICLE)).willReturn(VEHICLE_RESPONSE);

    FuelRecordResponse first =
        service.saveReFuel(refuel("2026-09-01T10:00:00Z", "40.00", "1.50", 1000, true));
    assertThat(first.totalCost()).isEqualByComparingTo("60.00");
    assertThat(first.consumptionL100km()).isNull();

    service.saveReFuel(refuel("2026-09-02T10:00:00Z", "20.00", "1.50", 1200, false));

    FuelRecordResponse third =
        service.saveReFuel(refuel("2026-09-20T10:00:00Z", "30.00", "1.60", 1500, true));
    assertThat(third.consumptionL100km()).isEqualByComparingTo("10.00");

    FuelConsumptionResponse consumption = service.consumption(FROM, TO);
    assertThat(consumption.vehicles())
        .singleElement()
        .satisfies(
            vehicle -> {
              assertThat(vehicle.plate()).isEqualTo("1234ABC");
              assertThat(vehicle.liters()).isEqualByComparingTo("90.00");
              assertThat(vehicle.cost()).isEqualByComparingTo("138.00");
              assertThat(vehicle.distanceKm()).isEqualTo(500);
              assertThat(vehicle.avgL100km()).isEqualByComparingTo("10.00");
            });

    FuelStatsResponse stats = service.stats(FROM, TO, Granularity.DAY, null);
    assertThat(stats.series()).hasSize(30);
    assertThat(stats.series().get(0).liters()).isEqualByComparingTo("40.00");
    assertThat(stats.series().get(1).liters()).isEqualByComparingTo("20.00");
    assertThat(stats.series().get(2).liters()).isEqualByComparingTo("0");
    assertThat(stats.series().get(19).liters()).isEqualByComparingTo("30.00");
    assertThat(stats.totals().liters()).isEqualByComparingTo("90.00");
    assertThat(stats.totals().cost()).isEqualByComparingTo("138.00");
  }

  private FuelRecordRequest refuel(
      String refueledAt, String liters, String pricePerLiter, int odometerKm, boolean fullTank) {
    return new FuelRecordRequest(
        VEHICLE,
        Instant.parse(refueledAt),
        new BigDecimal(liters),
        new BigDecimal(pricePerLiter),
        odometerKm,
        fullTank,
        "Repsol");
  }
}
