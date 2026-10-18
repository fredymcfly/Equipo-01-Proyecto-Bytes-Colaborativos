package com.fleetcontrol.msmaintenance.seeder;

import com.fleetcontrol.msmaintenance.client.VehicleGateway;
import com.fleetcontrol.msmaintenance.config.properties.DemoProperties;
import com.fleetcontrol.msmaintenance.constants.DemoConstants;
import com.fleetcontrol.msmaintenance.exception.ApiException;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.entities.MaintenancePlan;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenanceOrderRepository;
import com.fleetcontrol.msmaintenance.repository.MaintenancePlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Generates the demo history when the service starts with the {@code demo} profile.
 *
 * <p>It creates two plans for each of the 25 demo vehicles (50 plans) and 40 completed orders over
 * the last {@code fleet.demo.days} days. Six more plans get an open order: five fall due within a
 * week and one is already late. It does nothing when there are plans already.
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

  private static final PlanTemplate OIL_CHANGE =
      new PlanTemplate(MaintenanceType.OIL_CHANGE, 15_000, 365, 180);

  private static final List<PlanTemplate> SECOND_PLANS =
      List.of(
          new PlanTemplate(MaintenanceType.TIRE_ROTATION, 10_000, 180, 90),
          new PlanTemplate(MaintenanceType.BRAKE_CHECK, 30_000, null, 320),
          new PlanTemplate(MaintenanceType.GENERAL_INSPECTION, null, 365, 150),
          new PlanTemplate(MaintenanceType.LEGAL_INSPECTION, null, 365, 60));

  private final MaintenancePlanRepository planRepository;
  private final MaintenanceOrderRepository orderRepository;
  private final VehicleGateway vehicleGateway;
  private final TransactionTemplate transactionTemplate;
  private final DemoProperties properties;
  private final Clock clock;

  @Override
  public void run(ApplicationArguments args) {
    Thread thread = new Thread(this::seedWhenReady, "demo-maintenance-seeder");
    thread.setDaemon(true);
    thread.start();
  }

  private void seedWhenReady() {
    for (int attempt = 1; attempt <= DemoConstants.RETRY_ATTEMPTS; attempt++) {
      if (trySeed()) {
        return;
      }
      try {
        Thread.sleep(DemoConstants.RETRY_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    log.warn("Maintenance demo data not seeded: ms-vehicles never answered. Restart me.");
  }

  /**
   * Seeds the plans and orders if there are none. The odometer of every demo vehicle is read from
   * ms-vehicles, so the plans are coherent with it and the daily job does not add extra orders.
   *
   * @return true when the work is done; false when ms-vehicles is not available yet
   */
  public boolean trySeed() {
    if (planRepository.count() > 0) {
      log.info("Maintenance demo data already present, nothing to seed");
      return true;
    }
    Map<UUID, Integer> odometers = new HashMap<>();
    try {
      for (int vehicle = 1; vehicle <= DemoConstants.VEHICLE_COUNT; vehicle++) {
        UUID id = vehicleId(vehicle);
        Integer odometerKm = vehicleGateway.getVehicle(id).odometerKm();
        odometers.put(id, odometerKm == null ? 0 : odometerKm);
      }
    } catch (ApiException e) {
      log.info("Waiting for the demo vehicles: {}", e.getMessage());
      return false;
    }
    transactionTemplate.executeWithoutResult(status -> seed(odometers));
    return true;
  }

  private void seed(Map<UUID, Integer> odometers) {
    Random random =
        new Random(properties.seed() == null ? DemoConstants.DEFAULT_SEED : properties.seed());
    int days = properties.days() == null ? DemoConstants.DEFAULT_DAYS : properties.days();
    LocalDate today = LocalDate.now(clock);
    Faker faker = new Faker(Locale.forLanguageTag("es"), random);

    List<MaintenancePlan> oilPlans = new ArrayList<>();
    List<MaintenancePlan> otherPlans = new ArrayList<>();
    for (int vehicle = 1; vehicle <= DemoConstants.VEHICLE_COUNT; vehicle++) {
      UUID vehicleId = vehicleId(vehicle);
      oilPlans.add(newPlan(vehicleId, OIL_CHANGE, odometers.get(vehicleId), today, faker));
      PlanTemplate second = SECOND_PLANS.get(vehicle % SECOND_PLANS.size());
      otherPlans.add(newPlan(vehicleId, second, odometers.get(vehicleId), today, faker));
    }

    // Persist first so every plan has its generated id before orders refer to it.
    planRepository.saveAll(allPlans(oilPlans, otherPlans));

    List<MaintenanceOrder> orders = new ArrayList<>();
    int openOrders = DemoConstants.DUE_SOON_ORDERS + DemoConstants.OVERDUE_ORDERS;
    for (int index = 0; index < openOrders; index++) {
      LocalDate scheduledFor =
          index < DemoConstants.DUE_SOON_ORDERS
              ? today.plusDays(1L + faker.number().numberBetween(0, DemoConstants.MAX_DAYS_AHEAD))
              : today.minusDays(3L + faker.number().numberBetween(0, DemoConstants.MAX_DAYS_AHEAD));
      MaintenancePlan plan = oilPlans.get(index);
      plan.registerDone(scheduledFor.minusDays(plan.getIntervalDays()), plan.getLastDoneKm());
      orders.add(newOrder(plan, scheduledFor, MaintenanceOrderStatus.PENDING));
    }

    List<MaintenancePlan> withHistory =
        new ArrayList<>(oilPlans.subList(openOrders, DemoConstants.VEHICLE_COUNT));
    withHistory.addAll(otherPlans);
    Collections.shuffle(withHistory, random);
    for (MaintenancePlan plan : withHistory.subList(0, DemoConstants.HISTORY_ORDERS)) {
      orders.add(completedOrder(plan, today, days, faker));
    }

    orderRepository.saveAll(orders);
    log.info(
        "Seeded {} maintenance plans and {} orders",
        oilPlans.size() + otherPlans.size(),
        orders.size());
  }

  private List<MaintenancePlan> allPlans(
      List<MaintenancePlan> oilPlans, List<MaintenancePlan> otherPlans) {
    List<MaintenancePlan> all = new ArrayList<>(oilPlans);
    all.addAll(otherPlans);
    return all;
  }

  private UUID vehicleId(int vehicle) {
    return UUID.nameUUIDFromBytes(("vehicle-" + vehicle).getBytes(StandardCharsets.UTF_8));
  }

  private MaintenancePlan newPlan(
      UUID vehicleId, PlanTemplate template, int odometerKm, LocalDate today, Faker faker) {
    MaintenancePlan plan = new MaintenancePlan();
    plan.setVehicleId(vehicleId);
    plan.setType(template.type());
    plan.setIntervalKm(template.intervalKm());
    plan.setIntervalDays(template.intervalDays());
    // The last service is recent enough for the plan not to fall due when the demo starts.
    int maxDaysAgo =
        template.intervalDays() == null
            ? DemoConstants.MAX_DAYS_SINCE_SERVICE
            : Math.min(
                DemoConstants.MAX_DAYS_SINCE_SERVICE,
                template.intervalDays() - DemoConstants.DUE_MARGIN_DAYS);
    int daysAgo =
        DemoConstants.MIN_DAYS_SINCE_SERVICE
            + faker.number().numberBetween(0, maxDaysAgo - DemoConstants.MIN_DAYS_SINCE_SERVICE);
    int kmWindow =
        template.intervalKm() == null
            ? DemoConstants.DAYS_ONLY_KM_WINDOW
            : (int) (template.intervalKm() * DemoConstants.KM_WINDOW_RATIO);
    int kmAgo = faker.number().numberBetween(0, Math.max(1, Math.min(kmWindow, odometerKm)));
    plan.registerDone(today.minusDays(daysAgo), odometerKm - kmAgo);
    return plan;
  }

  private MaintenanceOrder newOrder(
      MaintenancePlan plan, LocalDate scheduledFor, MaintenanceOrderStatus status) {
    MaintenanceOrder order = new MaintenanceOrder();
    order.setPlanId(plan.getId());
    order.setVehicleId(plan.getVehicleId());
    order.setType(plan.getType());
    order.setStatus(status);
    order.setScheduledFor(scheduledFor);
    return order;
  }

  private MaintenanceOrder completedOrder(
      MaintenancePlan plan, LocalDate today, int days, Faker faker) {
    LocalDate day = today.minusDays(1L + faker.number().numberBetween(0, days));
    int odometerKm = plan.getLastDoneKm();
    plan.registerDone(day, odometerKm);

    MaintenanceOrder order = newOrder(plan, day, MaintenanceOrderStatus.COMPLETED);
    int hour = 8 + faker.number().numberBetween(0, 9);
    int minute = faker.number().numberBetween(0, 60);
    order.setCompletedAt(day.atTime(hour, minute).toInstant(ZoneOffset.UTC));
    long workHours = 2L + faker.number().numberBetween(0, 4);
    order.setStartedAt(order.getCompletedAt().minus(Duration.ofHours(workHours)));
    order.setOdometerKm(odometerKm);
    order.setCost(randomCost(template(plan.getType()).baseCost(), faker));
    order.setWorkshop(faker.options().nextElement(DemoConstants.WORKSHOPS));
    order.setNotes("Mantenimiento programado");
    return order;
  }

  private PlanTemplate template(MaintenanceType type) {
    return type == OIL_CHANGE.type()
        ? OIL_CHANGE
        : SECOND_PLANS.stream().filter(t -> t.type() == type).findFirst().orElse(OIL_CHANGE);
  }

  private BigDecimal randomCost(int baseCost, Faker faker) {
    double factor = 0.85 + faker.random().nextDouble() * 0.3;
    return BigDecimal.valueOf(baseCost * factor).setScale(2, RoundingMode.HALF_UP);
  }

  /** Interval and typical cost of a kind of plan. */
  private record PlanTemplate(
      MaintenanceType type, Integer intervalKm, Integer intervalDays, int baseCost) {}
}
