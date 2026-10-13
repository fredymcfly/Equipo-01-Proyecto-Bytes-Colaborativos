package com.fleetcontrol.msfuel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.model.FuelType;
import com.fleetcontrol.msfuel.repository.projection.FuelPeriodAggregate;
import com.fleetcontrol.msfuel.repository.projection.FuelTotalsAggregate;
import com.fleetcontrol.msfuel.repository.projection.VehicleFuelAggregate;
import com.fleetcontrol.msfuel.support.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FuelRecordRepositoryTest extends PostgresIntegrationTest {

  private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-10-01T00:00:00Z");

  @Autowired private FuelRecordRepository repository;

  @Test
  void aggregatesLitersCostAndOdometerByVehicle() {
    UUID vehicle = UUID.randomUUID();
    save(vehicle, "2026-09-01T10:00:00Z", "40.00", "1.50", "60.00", 1000, true, null);
    save(vehicle, "2026-09-02T10:00:00Z", "20.00", "1.50", "30.00", 1200, false, "10.00");
    save(vehicle, "2026-09-20T10:00:00Z", "30.00", "1.60", "48.00", 1500, true, "12.00");

    List<VehicleFuelAggregate> aggregates = repository.aggregateByVehicle(FROM, TO);

    assertThat(aggregates)
        .singleElement()
        .satisfies(
            aggregate -> {
              assertThat(aggregate.vehicleId()).isEqualTo(vehicle);
              assertThat(aggregate.liters()).isEqualByComparingTo("90.00");
              assertThat(aggregate.cost()).isEqualByComparingTo("138.00");
              assertThat(aggregate.avgConsumptionL100km()).isEqualByComparingTo("11.00");
              assertThat(aggregate.minOdometerKm()).isEqualTo(1000);
              assertThat(aggregate.maxOdometerKm()).isEqualTo(1500);
            });
  }

  @Test
  void buildsDailySeriesFillingNothingOutsideTheData() {
    UUID vehicle = UUID.randomUUID();
    save(vehicle, "2026-09-01T10:00:00Z", "40.00", "1.50", "60.00", 1000, true, null);
    save(vehicle, "2026-09-20T10:00:00Z", "30.00", "1.60", "48.00", 1500, true, "12.00");

    List<FuelPeriodAggregate> series = repository.aggregateByPeriod(FROM, TO, "day");

    assertThat(series).hasSize(2);
    assertThat(series.get(0).period()).isEqualTo("2026-09-01");
    assertThat(series.get(0).liters()).isEqualByComparingTo("40.00");
    assertThat(series.get(1).period()).isEqualTo("2026-09-20");
    assertThat(series.get(1).liters()).isEqualByComparingTo("30.00");
  }

  @Test
  void totalsCoverTheWholeRange() {
    UUID vehicle = UUID.randomUUID();
    save(vehicle, "2026-09-01T10:00:00Z", "40.00", "1.50", "60.00", 1000, true, null);
    save(vehicle, "2026-09-20T10:00:00Z", "30.00", "1.60", "48.00", 1500, true, "12.00");

    FuelTotalsAggregate totals = repository.totals(FROM, TO);

    assertThat(totals.liters()).isEqualByComparingTo("70.00");
    assertThat(totals.cost()).isEqualByComparingTo("108.00");
  }

  private void save(
      UUID vehicleId,
      String refueledAt,
      String liters,
      String price,
      String total,
      int odometer,
      boolean fullTank,
      String consumption) {
    FuelRecord record = new FuelRecord();
    record.setVehicleId(vehicleId);
    record.setRefueledAt(Instant.parse(refueledAt));
    record.setFuelType(FuelType.DIESEL);
    record.setLiters(new BigDecimal(liters));
    record.setPricePerLiter(new BigDecimal(price));
    record.setTotalCost(new BigDecimal(total));
    record.setOdometerKm(odometer);
    record.setFullTank(fullTank);
    record.setConsumptionL100km(consumption == null ? null : new BigDecimal(consumption));
    repository.save(record);
    repository.flush();
  }
}
