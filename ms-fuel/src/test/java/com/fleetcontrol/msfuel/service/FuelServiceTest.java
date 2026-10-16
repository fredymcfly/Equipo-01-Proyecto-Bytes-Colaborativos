package com.fleetcontrol.msfuel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fleetcontrol.msfuel.client.VehicleClient;
import com.fleetcontrol.msfuel.client.dto.VehicleResponse;
import com.fleetcontrol.msfuel.dto.FuelConsumptionResponse;
import com.fleetcontrol.msfuel.dto.FuelRecordRequest;
import com.fleetcontrol.msfuel.dto.FuelRecordResponse;
import com.fleetcontrol.msfuel.dto.FuelStatsPeriod;
import com.fleetcontrol.msfuel.dto.FuelStatsResponse;
import com.fleetcontrol.msfuel.dto.Granularity;
import com.fleetcontrol.msfuel.dto.PageResponse;
import com.fleetcontrol.msfuel.exception.BusinessConflictException;
import com.fleetcontrol.msfuel.exception.InvalidRequestException;
import com.fleetcontrol.msfuel.mapper.FuelRecordMapper;
import com.fleetcontrol.msfuel.model.FuelRecord;
import com.fleetcontrol.msfuel.model.FuelType;
import com.fleetcontrol.msfuel.repository.FuelRecordRepository;
import com.fleetcontrol.msfuel.repository.projection.FuelPeriodAggregate;
import com.fleetcontrol.msfuel.repository.projection.FuelTotalsAggregate;
import com.fleetcontrol.msfuel.repository.projection.VehicleFuelAggregate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FuelServiceTest {

  private static final UUID VEHICLE = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final LocalDate FROM = LocalDate.parse("2026-09-07");
  private static final LocalDate TO = LocalDate.parse("2026-09-27");

  @Mock private FuelRecordRepository repository;
  @Mock private VehicleClient vehicleClient;
  @Mock private FuelRecordMapper mapper;

  @InjectMocks private FuelService service;

  @Test
  void fillsMissingPeriodsWithZero() {
    given(repository.aggregateByPeriod(any(), any(), eq("week")))
        .willReturn(
            List.of(
                new FuelPeriodAggregate(
                    "2026-09-14",
                    new BigDecimal("50.00"),
                    new BigDecimal("80.00"),
                    new BigDecimal("1.600"),
                    new BigDecimal("11.000"))));
    given(repository.totals(any(), any()))
        .willReturn(
            new FuelTotalsAggregate(
                new BigDecimal("50.00"),
                new BigDecimal("80.00"),
                new BigDecimal("1.600"),
                new BigDecimal("11.000")));

    FuelStatsResponse response = service.stats(FROM, TO, Granularity.WEEK, null);

    assertThat(response.series())
        .extracting(FuelStatsPeriod::period)
        .containsExactly(
            LocalDate.parse("2026-09-07"),
            LocalDate.parse("2026-09-14"),
            LocalDate.parse("2026-09-21"));
    assertThat(response.series().get(0).liters()).isEqualByComparingTo("0");
    assertThat(response.series().get(1).liters()).isEqualByComparingTo("50.00");
    assertThat(response.series().get(1).avgPricePerLiter()).isEqualByComparingTo("1.60");
    assertThat(response.series().get(2).cost()).isEqualByComparingTo("0");
  }

  @Test
  void alignsPeriodsToTheStartOfTheMonth() {
    given(repository.aggregateByPeriod(any(), any(), eq("month"))).willReturn(List.of());
    given(repository.totals(any(), any()))
        .willReturn(new FuelTotalsAggregate(null, null, null, null));

    FuelStatsResponse response =
        service.stats(
            LocalDate.parse("2026-09-15"), LocalDate.parse("2026-10-10"), Granularity.MONTH, null);

    assertThat(response.series())
        .extracting(FuelStatsPeriod::period)
        .containsExactly(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-01"));
    assertThat(response.totals().liters()).isEqualByComparingTo("0");
  }

  @Test
  void groupsStatsByVehicleWhenAsked() {
    given(repository.aggregateByPeriodAndVehicle(any(), any(), any(), eq(VEHICLE)))
        .willReturn(List.of());
    given(repository.totalsByVehicle(any(), any(), eq(VEHICLE)))
        .willReturn(new FuelTotalsAggregate(null, null, null, null));

    service.stats(FROM, TO, Granularity.DAY, VEHICLE);

    verify(repository).aggregateByPeriodAndVehicle(any(), any(), eq("day"), eq(VEHICLE));
  }

  @Test
  void reportsConsumptionPerVehicle() {
    given(repository.aggregateByVehicle(any(), any()))
        .willReturn(
            List.of(
                new VehicleFuelAggregate(
                    VEHICLE,
                    new BigDecimal("90.00"),
                    new BigDecimal("138.00"),
                    new BigDecimal("11.005"),
                    1000,
                    1500)));
    given(vehicleClient.getVehicle(VEHICLE)).willReturn(vehicle());

    FuelConsumptionResponse response = service.consumption(FROM, TO);

    assertThat(response.vehicles())
        .singleElement()
        .satisfies(
            vehicle -> {
              assertThat(vehicle.plate()).isEqualTo("1234ABC");
              assertThat(vehicle.type()).isEqualTo("TRUCK");
              assertThat(vehicle.distanceKm()).isEqualTo(500);
              assertThat(vehicle.avgL100km()).isEqualByComparingTo("11.01");
            });
  }

  @Test
  void rejectsInvertedRange() {
    assertThatThrownBy(() -> service.consumption(TO, FROM))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void rejectsListWithOnlyOneBound() {
    assertThatThrownBy(() -> service.list(null, FROM, null, 0, 20))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void listsWholeFleetWithoutFilters() {
    Page<FuelRecord> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    given(repository.findAll(any(Pageable.class))).willReturn(page);

    PageResponse<FuelRecordResponse> response = service.list(null, null, null, 0, 20);

    assertThat(response.totalElements()).isZero();
    assertThat(response.content()).isEmpty();
  }

  @Test
  void rejectsRefuelAboveTankCapacity() {
    given(vehicleClient.getVehicle(VEHICLE)).willReturn(vehicle());

    assertThatThrownBy(() -> service.saveReFuel(request(new BigDecimal("70.00"), 1000, false)))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void rejectsOdometerBelowTheLastRefuel() {
    given(vehicleClient.getVehicle(VEHICLE)).willReturn(vehicle());
    given(repository.findFirstByVehicleIdOrderByOdometerKmDesc(VEHICLE))
        .willReturn(Optional.of(record(VEHICLE, 2000, new BigDecimal("40.00"))));

    assertThatThrownBy(() -> service.saveReFuel(request(new BigDecimal("40.00"), 1500, false)))
        .isInstanceOf(BusinessConflictException.class);
  }

  @Test
  void computesConsumptionFromThePreviousFullTank() {
    FuelRecord entity = new FuelRecord();
    given(vehicleClient.getVehicle(VEHICLE)).willReturn(vehicle());
    given(repository.findFirstByVehicleIdOrderByOdometerKmDesc(VEHICLE))
        .willReturn(Optional.of(record(VEHICLE, 1000, new BigDecimal("40.00"))));
    given(
            repository
                .findFirstByVehicleIdAndFullTankTrueAndOdometerKmLessThanOrderByOdometerKmDesc(
                    VEHICLE, 1500))
        .willReturn(Optional.of(record(VEHICLE, 1000, new BigDecimal("40.00"))));
    given(
            repository
                .findByVehicleIdAndOdometerKmGreaterThanAndOdometerKmLessThanOrderByOdometerKmAsc(
                    VEHICLE, 1000, 1500))
        .willReturn(List.of(record(VEHICLE, 1200, new BigDecimal("10.00"))));
    given(mapper.toEntity(any())).willReturn(entity);
    given(repository.save(entity)).willReturn(entity);
    given(mapper.toResponse(entity)).willReturn(null);

    service.saveReFuel(request(new BigDecimal("40.00"), 1500, true));

    assertThat(entity.getTotalCost()).isEqualByComparingTo("60.00");
    assertThat(entity.getConsumptionL100km()).isEqualByComparingTo("10.00");
    assertThat(entity.getFuelType()).isEqualTo(FuelType.DIESEL);
  }

  private static FuelRecordRequest request(BigDecimal liters, int odometerKm, boolean fullTank) {
    return new FuelRecordRequest(
        VEHICLE,
        Instant.parse("2026-09-01T10:00:00Z"),
        liters,
        new BigDecimal("1.50"),
        odometerKm,
        fullTank,
        null);
  }

  private static FuelRecord record(UUID vehicleId, int odometerKm, BigDecimal liters) {
    FuelRecord record = new FuelRecord();
    record.setVehicleId(vehicleId);
    record.setOdometerKm(odometerKm);
    record.setLiters(liters);
    record.setFullTank(true);
    return record;
  }

  private static VehicleResponse vehicle() {
    return new VehicleResponse(
        VEHICLE,
        "1234ABC",
        "Volvo",
        "FH16",
        2020,
        "TRUCK",
        "DIESEL",
        60,
        5000,
        "ACTIVE",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));
  }
}
