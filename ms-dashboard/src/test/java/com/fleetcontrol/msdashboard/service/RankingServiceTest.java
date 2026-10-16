package com.fleetcontrol.msdashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msdashboard.client.FuelClient;
import com.fleetcontrol.msdashboard.exception.ServiceUnavailableException;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msdashboard.models.dto.request.RankingQuery;
import com.fleetcontrol.msdashboard.models.dto.response.RankingEntryResponse;
import com.fleetcontrol.msdashboard.models.dto.response.RankingResponse;
import com.fleetcontrol.msdashboard.models.enums.RankingMetric;
import com.fleetcontrol.msdashboard.models.enums.VehicleType;
import com.fleetcontrol.msdashboard.support.FeignFailures;
import com.fleetcontrol.msdashboard.support.SourceData;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

  private static final String FROM = "2026-09-05";
  private static final String TO = "2026-10-05";

  @Mock private FuelClient fuelClient;

  private RankingService service;

  @BeforeEach
  void setUp() {
    service =
        new RankingService(
            fuelClient,
            new SourceCaller(Runnable::run),
            Clock.fixed(Instant.parse("2026-10-05T10:30:00Z"), ZoneOffset.UTC));
  }

  private RankingResponse rank(RankingMetric metric, int limit, VehicleType type) {
    return service.ranking(new RankingQuery(metric, 30, limit, type));
  }

  @Test
  void ranking_byFuelCost_sortsFromHighestToLowestAndNumbersFromOne() {
    when(fuelClient.consumption(FROM, TO)).thenReturn(SourceData.consumption());

    RankingResponse response = rank(RankingMetric.FUEL_COST, 5, null);

    assertThat(response.metric()).isEqualTo(RankingMetric.FUEL_COST);
    assertThat(response.unit()).isEqualTo("EUR");
    assertThat(response.period().from()).isEqualTo(LocalDate.parse(FROM));
    assertThat(response.period().to()).isEqualTo(LocalDate.parse(TO));
    assertThat(response.ranking())
        .extracting(RankingEntryResponse::rank, RankingEntryResponse::plate)
        .containsExactly(
            tuple(1, SourceData.TRUCK_PLATE),
            tuple(2, SourceData.VAN_PLATE),
            tuple(3, SourceData.CAR_PLATE));
    assertThat(response.ranking().get(0).value()).isEqualByComparingTo("2951.96");
  }

  @Test
  void ranking_byConsumption_usesTheAverageAndItsUnit() {
    when(fuelClient.consumption(FROM, TO)).thenReturn(SourceData.consumption());

    RankingResponse response = rank(RankingMetric.CONSUMPTION, 5, null);

    assertThat(response.unit()).isEqualTo("L/100 km");
    assertThat(response.ranking())
        .extracting(RankingEntryResponse::plate)
        .containsExactly(SourceData.TRUCK_PLATE, SourceData.VAN_PLATE, SourceData.CAR_PLATE);
    assertThat(response.ranking().get(2).value()).isEqualByComparingTo("7.2");
  }

  @Test
  void ranking_keepsOnlyTheLimit() {
    when(fuelClient.consumption(FROM, TO)).thenReturn(SourceData.consumption());

    RankingResponse response = rank(RankingMetric.FUEL_COST, 2, null);

    assertThat(response.ranking())
        .extracting(RankingEntryResponse::plate)
        .containsExactly(SourceData.TRUCK_PLATE, SourceData.VAN_PLATE);
  }

  @Test
  void ranking_withType_keepsOnlyVehiclesOfThatType() {
    when(fuelClient.consumption(FROM, TO)).thenReturn(SourceData.consumption());

    RankingResponse response = rank(RankingMetric.FUEL_COST, 5, VehicleType.VAN);

    assertThat(response.ranking())
        .extracting(RankingEntryResponse::plate)
        .containsExactly(SourceData.VAN_PLATE);
    assertThat(response.ranking().get(0).rank()).isEqualTo(1);
  }

  @Test
  void ranking_byConsumption_doesNotRankVehiclesWithoutDistance() {
    when(fuelClient.consumption(FROM, TO))
        .thenReturn(
            new FuelConsumptionDto(
                List.of(
                    SourceData.consumption(
                        SourceData.VAN_ID, SourceData.VAN_PLATE, "VAN", "100", null),
                    SourceData.consumption(
                        SourceData.CAR_ID, SourceData.CAR_PLATE, "CAR", "50", "7.2"))));

    RankingResponse response = rank(RankingMetric.CONSUMPTION, 5, null);

    assertThat(response.ranking())
        .extracting(RankingEntryResponse::plate)
        .containsExactly(SourceData.CAR_PLATE);
  }

  @Test
  void ranking_withEqualValues_ordersByPlate() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(fuelClient.consumption(FROM, TO))
        .thenReturn(
            new FuelConsumptionDto(
                List.of(
                    SourceData.consumption(second, "2222-BBB", "VAN", "100", "9.0"),
                    SourceData.consumption(first, "1111-AAA", "VAN", "100", "9.0"))));

    RankingResponse response = rank(RankingMetric.FUEL_COST, 5, null);

    assertThat(response.ranking())
        .extracting(RankingEntryResponse::plate)
        .containsExactly("1111-AAA", "2222-BBB");
  }

  @Test
  void ranking_withoutVehicles_isEmpty() {
    when(fuelClient.consumption(FROM, TO)).thenReturn(new FuelConsumptionDto(null));

    assertThat(rank(RankingMetric.FUEL_COST, 5, null).ranking()).isEmpty();
  }

  @Test
  void ranking_whenFuelFails_throwsServiceUnavailableNamingTheService() {
    when(fuelClient.consumption(FROM, TO)).thenThrow(FeignFailures.unavailable());

    assertThatThrownBy(() -> rank(RankingMetric.FUEL_COST, 5, null))
        .isInstanceOf(ServiceUnavailableException.class)
        .satisfies(
            exception -> {
              ServiceUnavailableException unavailable = (ServiceUnavailableException) exception;
              assertThat(unavailable.service()).isEqualTo("ms-fuel");
              assertThat(unavailable.status().value()).isEqualTo(503);
              assertThat(unavailable.code()).isEqualTo("SERVICE_UNAVAILABLE");
            });
  }
}
