package com.fleetcontrol.msdashboard.service;

import com.fleetcontrol.msdashboard.client.FuelClient;
import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import com.fleetcontrol.msdashboard.exception.ServiceUnavailableException;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msdashboard.models.dto.internal.FuelConsumptionDto.VehicleConsumption;
import com.fleetcontrol.msdashboard.models.dto.request.RankingQuery;
import com.fleetcontrol.msdashboard.models.dto.response.PeriodResponse;
import com.fleetcontrol.msdashboard.models.dto.response.RankingEntryResponse;
import com.fleetcontrol.msdashboard.models.dto.response.RankingResponse;
import com.fleetcontrol.msdashboard.models.enums.RankingMetric;
import com.fleetcontrol.msdashboard.models.enums.VehicleType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Ranks the vehicles by fuel cost or by consumption using the data of ms-fuel. */
@Service
@RequiredArgsConstructor
public class RankingService {

  private final FuelClient fuelClient;
  private final SourceCaller caller;
  private final Clock clock;

  /**
   * Vehicles sorted from the highest to the lowest value of the metric.
   *
   * @throws ServiceUnavailableException if ms-fuel does not answer, since nothing can be ranked
   */
  @Cacheable(cacheNames = BusinessConstants.Cache.RANKING, key = "#query")
  public RankingResponse ranking(RankingQuery query) {
    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(query.days());
    SourceResult<FuelConsumptionDto> result =
        caller
            .call(
                ConfigConstants.Services.FUEL,
                () -> fuelClient.consumption(from.toString(), to.toString()))
            .join();
    if (!result.isAvailable()) {
      throw new ServiceUnavailableException(result.service());
    }
    List<VehicleConsumption> sorted = sortedVehicles(result.data(), query);
    List<RankingEntryResponse> ranking =
        IntStream.range(0, sorted.size())
            .mapToObj(index -> toEntry(sorted.get(index), index, query.metric()))
            .toList();
    return new RankingResponse(
        query.metric(), query.metric().getUnit(), new PeriodResponse(from, to), ranking);
  }

  private List<VehicleConsumption> sortedVehicles(
      FuelConsumptionDto consumption, RankingQuery query) {
    RankingMetric metric = query.metric();
    List<VehicleConsumption> vehicles =
        consumption.vehicles() == null ? List.of() : consumption.vehicles();
    Comparator<VehicleConsumption> byValue =
        Comparator.comparing(
            (VehicleConsumption vehicle) -> valueOf(vehicle, metric), Comparator.reverseOrder());
    Comparator<VehicleConsumption> order =
        byValue.thenComparing(
            VehicleConsumption::plate, Comparator.nullsLast(Comparator.naturalOrder()));
    return vehicles.stream()
        .filter(vehicle -> matchesType(vehicle, query.type()))
        .filter(vehicle -> valueOf(vehicle, metric) != null)
        .sorted(order)
        .limit(query.limit())
        .toList();
  }

  private RankingEntryResponse toEntry(
      VehicleConsumption vehicle, int index, RankingMetric metric) {
    return new RankingEntryResponse(
        BusinessConstants.Ranking.FIRST_RANK + index,
        vehicle.vehicleId(),
        vehicle.plate(),
        vehicle.type(),
        valueOf(vehicle, metric));
  }

  private boolean matchesType(VehicleConsumption vehicle, VehicleType type) {
    return type == null || type.name().equalsIgnoreCase(vehicle.type());
  }

  /** Value used to rank. A vehicle without it, such as one with no distance, is not ranked. */
  private BigDecimal valueOf(VehicleConsumption vehicle, RankingMetric metric) {
    return switch (metric) {
      case FUEL_COST -> vehicle.cost();
      case CONSUMPTION -> vehicle.avgL100km();
    };
  }
}
