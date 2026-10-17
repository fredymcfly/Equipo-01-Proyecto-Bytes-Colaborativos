package com.fleetcontrol.msalerts.service.detector;

import com.fleetcontrol.msalerts.client.FuelClient;
import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.internal.FuelConsumptionDto;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Detects vehicles whose consumption of the last 30 days is well above the one of the 60 days
 * before, using ms-fuel.
 */
@Component
@RequiredArgsConstructor
public class ConsumptionDetector implements AlertDetector {

  private static final BigDecimal HUNDRED =
      BigDecimal.valueOf(BusinessConstants.Evaluation.PERCENT);

  private final FuelClient fuelClient;

  @Override
  public Set<AlertType> types() {
    return Set.of(AlertType.HIGH_CONSUMPTION);
  }

  @Override
  public List<Detection> detect(
      Map<AlertType, AlertRule> rules, LocalDate today, Function<UUID, String> plates) {
    AlertRule rule = rules.get(AlertType.HIGH_CONSUMPTION);
    int threshold =
        RuleParams.intValue(
            rule,
            BusinessConstants.RuleParams.THRESHOLD_PERCENT,
            BusinessConstants.RuleParams.DEFAULT_THRESHOLD_PERCENT);
    LocalDate recentFrom = today.minusDays(BusinessConstants.Evaluation.RECENT_DAYS);
    LocalDate previousTo = recentFrom.minusDays(1);
    LocalDate previousFrom = previousTo.minusDays(BusinessConstants.Evaluation.PREVIOUS_DAYS - 1);
    FuelConsumptionDto recent = fuelClient.consumption(recentFrom.toString(), today.toString());
    FuelConsumptionDto previous =
        fuelClient.consumption(previousFrom.toString(), previousTo.toString());
    Map<UUID, BigDecimal> previousAverages = averages(previous);
    return vehicles(recent).stream()
        .filter(vehicle -> vehicle.vehicleId() != null && vehicle.avgL100km() != null)
        .filter(
            vehicle ->
                isHigher(vehicle.avgL100km(), previousAverages.get(vehicle.vehicleId()), threshold))
        .map(
            vehicle ->
                build(rule, vehicle, previousAverages.get(vehicle.vehicleId()), plates))
        .toList();
  }

  private List<FuelConsumptionDto.VehicleConsumption> vehicles(FuelConsumptionDto dto) {
    return dto == null || dto.vehicles() == null ? List.of() : dto.vehicles();
  }

  private Map<UUID, BigDecimal> averages(FuelConsumptionDto dto) {
    Map<UUID, BigDecimal> averages = new HashMap<>();
    for (FuelConsumptionDto.VehicleConsumption vehicle : vehicles(dto)) {
      if (vehicle.vehicleId() != null && vehicle.avgL100km() != null) {
        averages.put(vehicle.vehicleId(), vehicle.avgL100km());
      }
    }
    return averages;
  }

  /** True if the recent value exceeds the previous one by more than the threshold percentage. */
  private boolean isHigher(BigDecimal recent, BigDecimal previous, int threshold) {
    if (previous == null || previous.signum() <= 0) {
      return false;
    }
    BigDecimal factor = BigDecimal.valueOf(BusinessConstants.Evaluation.PERCENT + (long) threshold);
    BigDecimal limit = previous.multiply(factor);
    return recent.multiply(HUNDRED).compareTo(limit) > 0;
  }

  private Detection build(
      AlertRule rule,
      FuelConsumptionDto.VehicleConsumption vehicle,
      BigDecimal previous,
      Function<UUID, String> plates) {
    BigDecimal rise =
        vehicle
            .avgL100km()
            .subtract(previous)
            .multiply(HUNDRED)
            .divide(previous, 0, RoundingMode.HALF_UP);
    String plate = vehicle.plate() != null ? vehicle.plate() : plates.apply(vehicle.vehicleId());
    String message =
        String.format(
            BusinessConstants.Messages.HIGH_CONSUMPTION,
            plate,
            rise.toPlainString(),
            vehicle.avgL100km().setScale(1, RoundingMode.HALF_UP).toPlainString(),
            previous.setScale(1, RoundingMode.HALF_UP).toPlainString());
    return new Detection(
        AlertType.HIGH_CONSUMPTION,
        rule.getSeverity(),
        vehicle.vehicleId(),
        null,
        message,
        AlertType.HIGH_CONSUMPTION + ":" + vehicle.vehicleId());
  }
}
