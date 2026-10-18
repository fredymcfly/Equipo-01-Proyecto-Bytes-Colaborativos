package com.fleetcontrol.msdrivers.seeder;

import com.fleetcontrol.msdrivers.config.properties.DemoProperties;
import com.fleetcontrol.msdrivers.constants.ConfigConstants;
import com.fleetcontrol.msdrivers.constants.DemoConstants;
import com.fleetcontrol.msdrivers.models.entities.Driver;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import com.fleetcontrol.msdrivers.repository.DriverRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates the demo drivers when the service starts with the {@code demo} profile.
 *
 * <p>It creates 20 drivers: 3 with a license that is about to expire, 1 with an expired license and
 * 16 with a license that is valid for a long time. The same {@code fleet.demo.seed} always produces
 * the same names, licenses and statuses, and nothing is inserted when there are drivers already.
 * The ids are generated when the drivers are saved.
 */
@Component
@Profile(ConfigConstants.DEMO_PROFILE)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

  private final DriverRepository repository;
  private final DemoProperties properties;
  private final Clock clock;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (repository.count() > 0) {
      log.info("Driver demo data already present, nothing to seed");
      return;
    }
    Random random =
        new Random(properties.seed() == null ? DemoConstants.DEFAULT_SEED : properties.seed());
    int days = properties.days() == null ? DemoConstants.DEFAULT_DAYS : properties.days();
    Faker faker = new Faker(Locale.forLanguageTag("es"), random);

    List<LocalDate> expirations = expirationDates(faker, random, LocalDate.now(clock));
    List<DriverStatus> statuses = statusPool(random);
    List<LicenseCategory> categories = categoryPool(random);
    Set<String> names = new HashSet<>();
    Set<String> licenses = new HashSet<>();
    List<Driver> drivers = new ArrayList<>();
    for (int index = 0; index < DemoConstants.DRIVER_COUNT; index++) {
      drivers.add(
          newDriver(
              uniqueName(faker, names),
              categories.get(index),
              expirations.get(index),
              statuses.get(index),
              licenses,
              days,
              faker));
    }
    repository.saveAll(drivers);
    assignDeterministicIds(drivers);
    log.info("Seeded {} demo drivers", drivers.size());
  }

  private Driver newDriver(
      String[] name,
      LicenseCategory category,
      LocalDate expiresAt,
      DriverStatus status,
      Set<String> licenses,
      int days,
      Faker faker) {
    Driver driver = new Driver();
    driver.setFullName(name[0] + " " + name[1]);
    driver.setEmail(String.format(DemoConstants.EMAIL_FORMAT, ascii(name[0]), ascii(name[1])));
    driver.setPhone(
        String.format(
            DemoConstants.PHONE_FORMAT,
            faker.number().numberBetween(0, DemoConstants.PHONE_PREFIX_BOUND),
            faker.number().numberBetween(0, DemoConstants.PHONE_PART_BOUND),
            faker.number().numberBetween(0, DemoConstants.PHONE_PART_BOUND)));
    driver.setLicenseNumber(uniqueLicense(faker, category, licenses));
    driver.setLicenseCategory(category);
    driver.setLicenseExpiresAt(expiresAt);
    driver.setStatus(status);
    int daysAgo = faker.number().numberBetween(0, Math.max(days, 1));
    Instant createdAt = clock.instant().minus(Duration.ofDays(1L + daysAgo));
    driver.setCreatedAt(createdAt);
    driver.setUpdatedAt(createdAt);
    return driver;
  }

  /** Expiration dates of the demo: the ones about to expire, the expired one and the valid ones. */
  private List<LocalDate> expirationDates(Faker faker, Random random, LocalDate today) {
    List<LocalDate> dates = new ArrayList<>();
    DemoConstants.EXPIRING_IN_DAYS.forEach(offset -> dates.add(today.plusDays(offset)));
    dates.add(today.minusDays(DemoConstants.EXPIRED_DAYS_AGO));
    while (dates.size() < DemoConstants.DRIVER_COUNT) {
      dates.add(
          today.plusDays(
              DemoConstants.VALID_MIN_DAYS
                  + faker.number().numberBetween(0, DemoConstants.VALID_RANGE_DAYS)));
    }
    Collections.shuffle(dates, random);
    return dates;
  }

  /** Statuses in the proportions of the demo, shuffled with the seed. */
  private List<DriverStatus> statusPool(Random random) {
    List<DriverStatus> statuses = new ArrayList<>();
    for (DemoConstants.StatusCount count : DemoConstants.STATUS_COUNTS) {
      statuses.addAll(Collections.nCopies(count.count(), count.status()));
    }
    Collections.shuffle(statuses, random);
    return statuses;
  }

  /** License categories in the proportions of the demo, shuffled with the seed. */
  private List<LicenseCategory> categoryPool(Random random) {
    List<LicenseCategory> categories = new ArrayList<>();
    for (DemoConstants.CategoryCount count : DemoConstants.CATEGORY_COUNTS) {
      categories.addAll(Collections.nCopies(count.count(), count.category()));
    }
    Collections.shuffle(categories, random);
    return categories;
  }

  private String[] uniqueName(Faker faker, Set<String> used) {
    String first;
    String last;
    do {
      first = faker.name().firstName();
      last = faker.name().lastName();
    } while (!used.add(first + " " + last));
    return new String[] {first, last};
  }

  private String uniqueLicense(Faker faker, LicenseCategory category, Set<String> used) {
    String license;
    do {
      license =
          String.format(
              DemoConstants.LICENSE_FORMAT,
              category.name(),
              faker.number().numberBetween(0, DemoConstants.LICENSE_DIGITS_BOUND));
    } while (!used.add(license));
    return license;
  }

  private static String ascii(String text) {
    String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
    return DemoConstants.DIACRITICS
        .matcher(decomposed)
        .replaceAll("")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z]", "");
  }

  /**
   * Gives the driver number N the id {@code driver-N}, in the order they were generated. The ids
   * are generated when saving, and a generated id cannot be set beforehand, so they are replaced
   * once the drivers are stored.
   */
  private void assignDeterministicIds(List<Driver> drivers) {
    for (int index = 0; index < drivers.size(); index++) {
      repository.reassignId(drivers.get(index).getId(), DemoConstants.driverId(index + 1));
    }
  }
}
