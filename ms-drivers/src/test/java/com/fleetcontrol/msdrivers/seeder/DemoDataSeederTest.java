package com.fleetcontrol.msdrivers.seeder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msdrivers.config.properties.DemoProperties;
import com.fleetcontrol.msdrivers.models.entities.Driver;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import com.fleetcontrol.msdrivers.repository.DriverRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the demo data generation. */
@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-05T10:30:00Z"), ZoneOffset.UTC);
  private static final LocalDate TODAY = LocalDate.now(CLOCK);
  private static final int SEED = 42;
  private static final int DAYS = 90;

  @Mock private DriverRepository repository;
  @Captor private ArgumentCaptor<List<Driver>> saved;

  @Test
  void run_generatesTwentyDriversWithTheDemoDistribution() {
    List<Driver> drivers = seed(SEED);

    assertThat(drivers).hasSize(20);
    assertThat(count(drivers, Driver::getStatus))
        .containsEntry(DriverStatus.ACTIVE, 16L)
        .containsEntry(DriverStatus.ON_LEAVE, 3L)
        .containsEntry(DriverStatus.SUSPENDED, 1L);
    assertThat(count(drivers, Driver::getLicenseCategory))
        .containsEntry(LicenseCategory.A, 4L)
        .containsEntry(LicenseCategory.B, 10L)
        .containsEntry(LicenseCategory.C, 6L);
  }

  @Test
  void run_generatesThreeLicensesAboutToExpireAndOneExpired() {
    List<Driver> drivers = seed(SEED);

    long expired =
        drivers.stream().filter(driver -> driver.getLicenseExpiresAt().isBefore(TODAY)).count();
    long aboutToExpire =
        drivers.stream()
            .filter(driver -> !driver.getLicenseExpiresAt().isBefore(TODAY))
            .filter(driver -> !driver.getLicenseExpiresAt().isAfter(TODAY.plusDays(30)))
            .count();
    long farFromExpiring =
        drivers.stream()
            .filter(driver -> !driver.getLicenseExpiresAt().isBefore(TODAY.plusDays(90)))
            .count();

    assertThat(expired).isEqualTo(1);
    assertThat(aboutToExpire).isEqualTo(3);
    assertThat(farFromExpiring).isEqualTo(16);
  }

  @Test
  void run_generatesUniqueAndWellFormedData() {
    List<Driver> drivers = seed(SEED);

    assertThat(drivers).extracting(Driver::getLicenseNumber).doesNotHaveDuplicates();
    assertThat(drivers).extracting(Driver::getEmail).doesNotHaveDuplicates();
    assertThat(drivers).extracting(Driver::getFullName).doesNotHaveDuplicates();
    assertThat(drivers)
        .allSatisfy(
            driver -> {
              assertThat(driver.getLicenseNumber())
                  .matches("[ABC]-\\d{7}")
                  .startsWith(driver.getLicenseCategory().name());
              assertThat(driver.getEmail()).matches("[a-z]+\\.[a-z]+@fleetcontrol\\.com");
              assertThat(driver.getPhone()).matches("\\+34 6\\d{2} \\d{3} \\d{3}");
              assertThat(driver.getCreatedAt()).isBefore(CLOCK.instant());
              assertThat(driver.getCreatedAt())
                  .isAfterOrEqualTo(CLOCK.instant().minusSeconds(DAYS * 86_400L));
              assertThat(driver.getUpdatedAt()).isEqualTo(driver.getCreatedAt());
            });
  }

  @Test
  void run_leavesTheIdsToBeGeneratedWhenTheDriversAreSaved() {
    assertThat(seed(SEED)).extracting(Driver::getId).containsOnlyNulls();
  }

  @Test
  void run_withTheSameSeed_producesTheSameData() {
    List<Driver> first = seed(SEED);
    List<Driver> second = seed(SEED);

    assertThat(second).usingRecursiveComparison().isEqualTo(first);
  }

  @Test
  void run_withAnotherSeed_changesTheData() {
    List<String> licenses = seed(SEED).stream().map(Driver::getLicenseNumber).toList();

    assertThat(seed(7)).extracting(Driver::getLicenseNumber).isNotEqualTo(licenses);
  }

  @Test
  void run_whenThereAreDrivers_insertsNothing() {
    when(repository.count()).thenReturn(1L);

    new DemoDataSeeder(repository, new DemoProperties(SEED, DAYS), CLOCK).run(null);

    verify(repository, never()).saveAll(anyList());
  }

  private List<Driver> seed(int seed) {
    when(repository.count()).thenReturn(0L);
    new DemoDataSeeder(repository, new DemoProperties(seed, DAYS), CLOCK).run(null);
    verify(repository, atLeastOnce()).saveAll(saved.capture());
    return saved.getValue();
  }

  private static <K> Map<K, Long> count(List<Driver> drivers, Function<Driver, K> key) {
    return drivers.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
  }
}
