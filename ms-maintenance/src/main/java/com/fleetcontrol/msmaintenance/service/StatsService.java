package com.fleetcontrol.msmaintenance.service;

import com.fleetcontrol.msmaintenance.constants.BusinessConstants;
import com.fleetcontrol.msmaintenance.exception.ValidationFailedException;
import com.fleetcontrol.msmaintenance.models.dto.response.ErrorDetail;
import com.fleetcontrol.msmaintenance.models.dto.response.StatsResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.enums.Granularity;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceOrderStatus;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenanceOrderRepository;
import com.fleetcontrol.msmaintenance.repository.specifications.OrderSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the statistics of completed orders consumed by ms-dashboard. */
@Service
@RequiredArgsConstructor
public class StatsService {

  private final MaintenanceOrderRepository orderRepository;
  private final Clock clock;

  /**
   * Returns the completed orders of the range grouped by period and by type, plus the pending
   * orders that are late or due within seven days. Periods without orders are included with zero.
   */
  @Transactional(readOnly = true)
  public StatsResponse stats(
      LocalDate from, LocalDate to, Granularity granularity, UUID vehicleId) {
    if (from.isAfter(to)) {
      throw new ValidationFailedException(List.of(new ErrorDetail("from", "must not be after to")));
    }
    List<MaintenanceOrder> orders = findCompleted(from, to, vehicleId);

    Map<LocalDate, Accumulator> periods = emptyPeriods(from, to, granularity);
    Map<MaintenanceType, Accumulator> types = new EnumMap<>(MaintenanceType.class);
    Accumulator totals = new Accumulator();
    for (MaintenanceOrder order : orders) {
      LocalDate day = LocalDate.ofInstant(order.getCompletedAt(), ZoneOffset.UTC);
      periods.get(periodStart(day, granularity)).add(order.getCost());
      types.computeIfAbsent(order.getType(), type -> new Accumulator()).add(order.getCost());
      totals.add(order.getCost());
    }
    return new StatsResponse(
        from,
        to,
        granularity,
        new StatsResponse.Totals(totals.orders, totals.cost),
        periods.entrySet().stream()
            .map(
                e ->
                    new StatsResponse.SeriesPoint(
                        e.getKey(), e.getValue().orders, e.getValue().cost))
            .toList(),
        byType(types),
        upcoming(vehicleId));
  }

  private List<MaintenanceOrder> findCompleted(LocalDate from, LocalDate to, UUID vehicleId) {
    Specification<MaintenanceOrder> filters =
        OrderSpecifications.hasStatus(MaintenanceOrderStatus.COMPLETED)
            .and(OrderSpecifications.hasVehicle(vehicleId))
            .and(
                OrderSpecifications.completedBetween(
                    from.atStartOfDay().toInstant(ZoneOffset.UTC),
                    to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
    return orderRepository.findAll(filters);
  }

  private Map<LocalDate, Accumulator> emptyPeriods(
      LocalDate from, LocalDate to, Granularity granularity) {
    Map<LocalDate, Accumulator> periods = new TreeMap<>();
    LocalDate period = periodStart(from, granularity);
    while (!period.isAfter(to)) {
      periods.put(period, new Accumulator());
      period = nextPeriod(period, granularity);
    }
    return periods;
  }

  private LocalDate periodStart(LocalDate day, Granularity granularity) {
    return switch (granularity) {
      case DAY -> day;
      case WEEK -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> day.withDayOfMonth(1);
    };
  }

  private LocalDate nextPeriod(LocalDate period, Granularity granularity) {
    return switch (granularity) {
      case DAY -> period.plusDays(1);
      case WEEK -> period.plusWeeks(1);
      case MONTH -> period.plusMonths(1);
    };
  }

  private List<StatsResponse.TypeStats> byType(Map<MaintenanceType, Accumulator> types) {
    List<StatsResponse.TypeStats> result = new ArrayList<>();
    types.forEach(
        (type, accumulator) ->
            result.add(new StatsResponse.TypeStats(type, accumulator.orders, accumulator.cost)));
    result.sort(
        Comparator.comparingLong(StatsResponse.TypeStats::orders)
            .reversed()
            .thenComparing(StatsResponse.TypeStats::cost, Comparator.reverseOrder())
            .thenComparing(StatsResponse.TypeStats::type));
    return result;
  }

  private StatsResponse.Upcoming upcoming(UUID vehicleId) {
    LocalDate today = LocalDate.now(clock);
    Specification<MaintenanceOrder> pending =
        OrderSpecifications.hasStatus(MaintenanceOrderStatus.PENDING)
            .and(OrderSpecifications.hasVehicle(vehicleId));
    long overdue = orderRepository.count(pending.and(OrderSpecifications.scheduledBefore(today)));
    long dueSoon =
        orderRepository.count(
            pending.and(
                OrderSpecifications.scheduledBetween(
                    today, today.plusDays(BusinessConstants.UPCOMING_DAYS))));
    return new StatsResponse.Upcoming(overdue, dueSoon);
  }

  /** Counts orders and adds up their cost. */
  private static final class Accumulator {
    private long orders;
    private BigDecimal cost = BigDecimal.ZERO.setScale(BusinessConstants.MONEY_SCALE);

    void add(BigDecimal amount) {
      orders++;
      if (amount != null) {
        cost = cost.add(amount).setScale(BusinessConstants.MONEY_SCALE, RoundingMode.HALF_UP);
      }
    }
  }
}
