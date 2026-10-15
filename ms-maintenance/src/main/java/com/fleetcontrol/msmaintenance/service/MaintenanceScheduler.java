package com.fleetcontrol.msmaintenance.service;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the daily check of the maintenance plans at 06:00 UTC. */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceScheduler {

  private final MaintenanceOrderGenerator generator;
  private final Clock clock;

  /** Creates the orders of the plans that are about to fall due. */
  @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
  public void generateDueOrders() {
    int created = generator.generate(LocalDate.now(clock));
    log.info("Daily maintenance check created {} orders", created);
  }
}
