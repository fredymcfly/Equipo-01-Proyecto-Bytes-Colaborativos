package com.fleetcontrol.msdashboard.support;

import com.fleetcontrol.msdashboard.models.dto.internal.AlertsStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FleetSummaryDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelConsumptionDto.VehicleConsumption;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceOrdersDto;
import com.fleetcontrol.msdashboard.models.dto.internal.MaintenanceStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.RoutesStatsDto;
import com.fleetcontrol.msdashboard.models.dto.internal.VehicleDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Answers of the source services used by the tests, taken from the documentation examples. */
public final class SourceData {

  public static final UUID VAN_ID = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01");
  public static final UUID CAR_ID = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a02");
  public static final UUID TRUCK_ID = UUID.fromString("6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03");
  public static final UUID ORDER_ID = UUID.fromString("e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01");
  public static final String VAN_PLATE = "4821-KDF";
  public static final String CAR_PLATE = "7733-LMN";
  public static final String TRUCK_PLATE = "9012-PQR";

  private SourceData() {}

  /** Fleet of 25 vehicles. */
  public static FleetSummaryDto fleet() {
    return new FleetSummaryDto(
        25,
        Map.of("AVAILABLE", 14L, "IN_USE", 8L, "IN_MAINTENANCE", 2L, "OUT_OF_SERVICE", 1L),
        Map.of("VAN", 10L, "TRUCK", 15L));
  }

  /** Vehicle with the given id and plate. */
  public static VehicleDto vehicle(UUID id, String plate) {
    return new VehicleDto(id, plate);
  }

  /** Routes of three weeks. */
  public static RoutesStatsDto routesStats() {
    return new RoutesStatsDto(
        new RoutesStatsDto.Totals(412, new BigDecimal("38420.5")),
        List.of(
            new RoutesStatsDto.Point("2026-09-07", 97, new BigDecimal("9034.1")),
            new RoutesStatsDto.Point("2026-09-14", 104, new BigDecimal("9710.6")),
            new RoutesStatsDto.Point("2026-09-21", 101, new BigDecimal("9340.2"))));
  }

  /** Fuel of three weeks. */
  public static FuelStatsDto fuelStats() {
    return new FuelStatsDto(
        new FuelStatsDto.Totals(
            new BigDecimal("4226.3"), new BigDecimal("6846.61"), new BigDecimal("11.0")),
        List.of(
            new FuelStatsDto.Point(
                "2026-09-07",
                new BigDecimal("984.7"),
                new BigDecimal("1595.21"),
                new BigDecimal("10.9")),
            new FuelStatsDto.Point(
                "2026-09-14",
                new BigDecimal("1077.9"),
                new BigDecimal("1746.20"),
                new BigDecimal("11.1"))));
  }

  /** Maintenance of two weeks. */
  public static MaintenanceStatsDto maintenanceStats() {
    return new MaintenanceStatsDto(
        new MaintenanceStatsDto.Totals(9, new BigDecimal("2140.25")),
        List.of(
            new MaintenanceStatsDto.Point("2026-09-07", 3, new BigDecimal("420.00")),
            new MaintenanceStatsDto.Point("2026-09-14", 4, new BigDecimal("715.25"))));
  }

  /** Alerts of two weeks, with 11 open. */
  public static AlertsStatsDto alertsStats() {
    return new AlertsStatsDto(
        new AlertsStatsDto.Totals(8),
        List.of(
            new AlertsStatsDto.Point("2026-09-07", 3), new AlertsStatsDto.Point("2026-09-14", 5)),
        Map.of("INFO", 0L, "WARNING", 9L, "CRITICAL", 2L));
  }

  /** One pending order of the van. */
  public static MaintenanceOrdersDto orders() {
    return new MaintenanceOrdersDto(
        List.of(
            new MaintenanceOrdersDto.Order(
                ORDER_ID, VAN_ID, "OIL_CHANGE", LocalDate.parse("2026-10-08"))));
  }

  /** Consumption of a truck, a van and a car, not sorted. */
  public static FuelConsumptionDto consumption() {
    return new FuelConsumptionDto(
        List.of(
            consumption(VAN_ID, VAN_PLATE, "VAN", "1118.29", "10.9"),
            consumption(TRUCK_ID, TRUCK_PLATE, "TRUCK", "2951.96", "27.5"),
            consumption(CAR_ID, CAR_PLATE, "CAR", "512.40", "7.2")));
  }

  /** Consumption of one vehicle. A null average means that it has no distance. */
  public static VehicleConsumption consumption(
      UUID id, String plate, String type, String cost, String avgL100km) {
    return new VehicleConsumption(
        id,
        plate,
        type,
        new BigDecimal("100.0"),
        new BigDecimal(cost),
        new BigDecimal("1000"),
        avgL100km == null ? null : new BigDecimal(avgL100km));
  }
}
