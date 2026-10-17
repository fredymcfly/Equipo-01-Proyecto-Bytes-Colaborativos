package com.fleetcontrol.msalerts.service.detector;

import com.fleetcontrol.msalerts.client.DriversClient;
import com.fleetcontrol.msalerts.constants.BusinessConstants;
import com.fleetcontrol.msalerts.models.dto.internal.Detection;
import com.fleetcontrol.msalerts.models.dto.internal.DriversPageDto;
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

/** Detects licenses that are about to expire or have expired, using ms-drivers. */
@Component
@RequiredArgsConstructor
public class LicenseDetector implements AlertDetector {

  private final DriversClient driversClient;

  @Override
  public Set<AlertType> types() {
    return Set.of(AlertType.LICENSE_EXPIRING, AlertType.LICENSE_EXPIRED);
  }

  /**
   * ms-drivers returns the licenses that expire within the given days, which includes the expired
   * ones. With 0 days only the licenses that expire today or earlier are returned.
   */
  @Override
  public List<Detection> detect(
      Map<AlertType, AlertRule> rules, LocalDate today, Function<UUID, String> plates) {
    AlertRule expiring = rules.get(AlertType.LICENSE_EXPIRING);
    AlertRule expired = rules.get(AlertType.LICENSE_EXPIRED);
    int days =
        expiring == null
            ? 0
            : RuleParams.intValue(
                expiring,
                BusinessConstants.RuleParams.DAYS_BEFORE,
                BusinessConstants.RuleParams.DEFAULT_LICENSE_DAYS);
    List<Detection> detections = new ArrayList<>();
    int page = BusinessConstants.Evaluation.FIRST_PAGE;
    int totalPages;
    do {
      DriversPageDto result =
          driversClient.expiringLicenses(days, page, BusinessConstants.Evaluation.PAGE_SIZE);
      List<DriversPageDto.Driver> drivers = result == null ? null : result.content();
      if (drivers != null) {
        for (DriversPageDto.Driver driver : drivers) {
          toDetection(driver, today, expiring, expired).ifPresent(detections::add);
        }
      }
      totalPages = result == null ? 0 : result.totalPages();
      page++;
    } while (page < totalPages);
    return detections;
  }

  private Optional<Detection> toDetection(
      DriversPageDto.Driver driver, LocalDate today, AlertRule expiring, AlertRule expired) {
    LocalDate expiresAt = driver.licenseExpiresAt();
    if (expiresAt == null) {
      return Optional.empty();
    }
    if (expiresAt.isBefore(today)) {
      return Optional.ofNullable(expired)
          .map(
              rule ->
                  build(
                      AlertType.LICENSE_EXPIRED,
                      rule,
                      driver,
                      BusinessConstants.Messages.LICENSE_EXPIRED));
    }
    return Optional.ofNullable(expiring)
        .map(
            rule ->
                build(
                    AlertType.LICENSE_EXPIRING,
                    rule,
                    driver,
                    BusinessConstants.Messages.LICENSE_EXPIRING));
  }

  private Detection build(
      AlertType type, AlertRule rule, DriversPageDto.Driver driver, String template) {
    String message = String.format(template, driver.fullName(), driver.licenseExpiresAt());
    return new Detection(
        type, rule.getSeverity(), null, driver.id(), message, type + ":" + driver.id());
  }
}
