package com.fleetcontrol.msmaintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msmaintenance.exception.ValidationFailedException;
import com.fleetcontrol.msmaintenance.models.dto.response.StatsResponse;
import com.fleetcontrol.msmaintenance.models.entities.MaintenanceOrder;
import com.fleetcontrol.msmaintenance.models.enums.Granularity;
import com.fleetcontrol.msmaintenance.models.enums.MaintenanceType;
import com.fleetcontrol.msmaintenance.repository.MaintenanceOrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);

  @Mock private MaintenanceOrderRepository orderRepository;

  private StatsService service;

  @BeforeEach
  void setUp() {
    service = new StatsService(orderRepository, CLOCK);
  }

  private static MaintenanceOrder completed(String completedAt, MaintenanceType type, String cost) {
    MaintenanceOrder order = new MaintenanceOrder();
    order.setType(type);
    order.setCompletedAt(Instant.parse(completedAt));
    order.setCost(new BigDecimal(cost));
    return order;
  }

  private void givenCompleted(MaintenanceOrder... orders) {
    when(orderRepository.findAll(ArgumentMatchers.<Specification<MaintenanceOrder>>any()))
        .thenReturn(List.of(orders));
  }

  @Test
  void stats_fromAfterTo_throwsValidationError() {
    LocalDate from = LocalDate.of(2026, 10, 10);
    LocalDate to = LocalDate.of(2026, 10, 1);

    assertThatThrownBy(() -> service.stats(from, to, Granularity.DAY, null))
        .isInstanceOf(ValidationFailedException.class);
  }

  @Test
  void stats_dailySeries_includesEmptyDaysAndTotals() {
    givenCompleted(
        completed("2026-10-02T10:00:00Z", MaintenanceType.OIL_CHANGE, "100"),
        completed("2026-10-02T15:00:00Z", MaintenanceType.OIL_CHANGE, "50.5"),
        completed("2026-10-05T09:00:00Z", MaintenanceType.BRAKE_CHECK, "200"));

    StatsResponse stats =
        service.stats(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), Granularity.DAY, null);

    assertThat(stats.series()).hasSize(7);
    assertThat(stats.series().get(0).orders()).isZero();
    assertThat(stats.series().get(1).period()).isEqualTo(LocalDate.of(2026, 10, 2));
    assertThat(stats.series().get(1).orders()).isEqualTo(2);
    assertThat(stats.series().get(1).cost()).isEqualByComparingTo("150.50");
    assertThat(stats.totals().orders()).isEqualTo(3);
    assertThat(stats.totals().cost()).isEqualByComparingTo("350.50");
  }

  @Test
  void stats_byType_isSortedByNumberOfOrders() {
    givenCompleted(
        completed("2026-10-02T10:00:00Z", MaintenanceType.BRAKE_CHECK, "500"),
        completed("2026-10-03T10:00:00Z", MaintenanceType.OIL_CHANGE, "40"),
        completed("2026-10-04T10:00:00Z", MaintenanceType.OIL_CHANGE, "60"));

    StatsResponse stats =
        service.stats(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), Granularity.DAY, null);

    assertThat(stats.byType()).hasSize(2);
    assertThat(stats.byType().get(0).type()).isEqualTo(MaintenanceType.OIL_CHANGE);
    assertThat(stats.byType().get(0).orders()).isEqualTo(2);
    assertThat(stats.byType().get(0).cost()).isEqualByComparingTo("100");
    assertThat(stats.byType().get(1).type()).isEqualTo(MaintenanceType.BRAKE_CHECK);
  }

  @Test
  void stats_weeklySeries_groupsByMonday() {
    givenCompleted(completed("2026-10-14T10:00:00Z", MaintenanceType.OIL_CHANGE, "75"));

    // 7 October 2026 is a Wednesday, so the first week starts on Monday the 5th.
    StatsResponse stats =
        service.stats(
            LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 20), Granularity.WEEK, null);

    assertThat(stats.series())
        .extracting(StatsResponse.SeriesPoint::period)
        .containsExactly(
            LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19));
    assertThat(stats.series().get(1).orders()).isEqualTo(1);
    assertThat(stats.series().get(1).cost()).isEqualByComparingTo("75");
  }

  @Test
  void stats_monthlySeries_groupsByFirstDayOfMonth() {
    givenCompleted(completed("2026-10-20T10:00:00Z", MaintenanceType.OIL_CHANGE, "10"));

    StatsResponse stats =
        service.stats(
            LocalDate.of(2026, 9, 15), LocalDate.of(2026, 11, 10), Granularity.MONTH, null);

    assertThat(stats.series())
        .extracting(StatsResponse.SeriesPoint::period)
        .containsExactly(
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
    assertThat(stats.series().get(1).orders()).isEqualTo(1);
  }

  @Test
  void stats_noOrders_returnsZerosAndUpcomingCounts() {
    givenCompleted();
    when(orderRepository.count(ArgumentMatchers.<Specification<MaintenanceOrder>>any()))
        .thenReturn(2L, 3L);

    StatsResponse stats =
        service.stats(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), Granularity.DAY, null);

    assertThat(stats.totals().orders()).isZero();
    assertThat(stats.totals().cost()).isEqualByComparingTo("0");
    assertThat(stats.byType()).isEmpty();
    assertThat(stats.upcoming().overdue()).isEqualTo(2);
    assertThat(stats.upcoming().dueIn7Days()).isEqualTo(3);
  }
}
