package com.fleetcontrol.msalerts.service.detector;

import com.fleetcontrol.msalerts.client.MaintenanceClient;
import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.internal.MaintenanceOrdersDto;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Detects pending maintenance orders that are close or late, using ms-maintenance. */
@Component
@RequiredArgsConstructor
public class MaintenanceDetector implements AlertDetector {

  private final MaintenanceClient maintenanceClient;

  @Override
  public Set<AlertType> types() {
    return Set.of(AlertType.MAINTENANCE_DUE, AlertType.MAINTENANCE_OVERDUE);
  }

  @Override
  public List<Detection> detect(
      Map<AlertType, AlertRule> rules, LocalDate today, Function<UUID, String> plates) {
    AlertRule due = rules.get(AlertType.MAINTENANCE_DUE);
    AlertRule overdue = rules.get(AlertType.MAINTENANCE_OVERDUE);
    LocalDate limit =
        due == null
            ? today.minusDays(1)
            : today.plusDays(
                RuleParams.intValue(
                    due,
                    BusinessConstants.RuleParams.DAYS_BEFORE,
                    BusinessConstants.RuleParams.DEFAULT_MAINTENANCE_DAYS));
    List<Detection> detections = new ArrayList<>();
    int page = BusinessConstants.Evaluation.FIRST_PAGE;
    int totalPages;
    do {
      MaintenanceOrdersDto result =
          maintenanceClient.orders(
              BusinessConstants.Evaluation.PENDING_STATUS,
              limit.toString(),
              page,
              BusinessConstants.Evaluation.PAGE_SIZE);
      List<MaintenanceOrdersDto.Order> orders = result == null ? null : result.content();
      if (orders != null) {
        for (MaintenanceOrdersDto.Order order : orders) {
          toDetection(order, today, limit, due, overdue, plates).ifPresent(detections::add);
        }
      }
      totalPages = result == null ? 0 : result.totalPages();
      page++;
    } while (page < totalPages);
    return detections;
  }

  private Optional<Detection> toDetection(
      MaintenanceOrdersDto.Order order,
      LocalDate today,
      LocalDate limit,
      AlertRule due,
      AlertRule overdue,
      Function<UUID, String> plates) {
    LocalDate scheduledFor = order.scheduledFor();
    if (scheduledFor == null) {
      return Optional.empty();
    }
    if (scheduledFor.isBefore(today)) {
      return Optional.ofNullable(overdue)
          .map(
              rule ->
                  build(
                      AlertType.MAINTENANCE_OVERDUE,
                      rule,
                      order,
                      BusinessConstants.Messages.MAINTENANCE_OVERDUE,
                      plates));
    }
    return Optional.ofNullable(due)
        .filter(rule -> !scheduledFor.isAfter(limit))
        .map(
            rule ->
                build(
                    AlertType.MAINTENANCE_DUE,
                    rule,
                    order,
                    BusinessConstants.Messages.MAINTENANCE_DUE,
                    plates));
  }

  private Detection build(
      AlertType type,
      AlertRule rule,
      MaintenanceOrdersDto.Order order,
      String template,
      Function<UUID, String> plates) {
    String message =
        String.format(
            template, order.type(), order.scheduledFor(), plates.apply(order.vehicleId()));
    return new Detection(
        type, rule.getSeverity(), order.vehicleId(), null, message, type + ":" + order.id());
  }
}
