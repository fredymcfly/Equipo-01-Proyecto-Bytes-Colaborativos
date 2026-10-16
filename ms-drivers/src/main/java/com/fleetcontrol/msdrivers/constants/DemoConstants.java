package com.fleetcontrol.msdrivers.constants;

import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import java.util.List;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the sizes, distributions and catalogue used to generate demo data. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoConstants {

  public static final int DEFAULT_SEED = 42;
  public static final int DEFAULT_DAYS = 90;

  /** Total number of drivers generated. */
  public static final int DRIVER_COUNT = 20;

  /** Days left until the expiration of the licenses that are about to expire: 3 drivers. */
  public static final List<Integer> EXPIRING_IN_DAYS = List.of(4, 13, 27);

  /** Days since the expiration of the only license that is already expired. */
  public static final int EXPIRED_DAYS_AGO = 18;

  /** The other licenses expire between this number of days from now and the next range. */
  public static final int VALID_MIN_DAYS = 90;

  public static final int VALID_RANGE_DAYS = 1_700;

  public static final String EMAIL_DOMAIN = "fleetcontrol.com";
  public static final String EMAIL_FORMAT = "%s.%s@" + EMAIL_DOMAIN;
  public static final String PHONE_FORMAT = "+34 6%02d %03d %03d";
  public static final String LICENSE_FORMAT = "%s-%07d";
  public static final int LICENSE_DIGITS_BOUND = 10_000_000;
  public static final int PHONE_PART_BOUND = 1_000;
  public static final int PHONE_PREFIX_BOUND = 100;

  /** Matches the accents of a normalized text, to remove them from the emails. */
  public static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

  /** How many drivers end up in a status. */
  public record StatusCount(DriverStatus status, int count) {}

  /** How many drivers have a license category. */
  public record CategoryCount(LicenseCategory category, int count) {}

  /** Drivers by status: 20 in total. */
  public static final List<StatusCount> STATUS_COUNTS =
      List.of(
          new StatusCount(DriverStatus.ACTIVE, 16),
          new StatusCount(DriverStatus.ON_LEAVE, 3),
          new StatusCount(DriverStatus.SUSPENDED, 1));

  /** Drivers by license category: 20 in total. */
  public static final List<CategoryCount> CATEGORY_COUNTS =
      List.of(
          new CategoryCount(LicenseCategory.A, 4),
          new CategoryCount(LicenseCategory.B, 10),
          new CategoryCount(LicenseCategory.C, 6));

  public static final List<String> FIRST_NAMES =
      List.of(
          "Laura",
          "Miguel",
          "Sara",
          "Carlos",
          "Elena",
          "Javier",
          "Lucía",
          "Pablo",
          "Marta",
          "Diego",
          "Carmen",
          "Alberto",
          "Nuria",
          "Sergio",
          "Irene",
          "Rubén",
          "Paula",
          "Andrés",
          "Beatriz",
          "Héctor",
          "Silvia",
          "Óscar",
          "Cristina",
          "Raúl");

  public static final List<String> LAST_NAMES =
      List.of(
          "Fernández",
          "Torres",
          "Ibáñez",
          "Ruiz",
          "Gómez",
          "Navarro",
          "Molina",
          "Castillo",
          "Ortega",
          "Romero",
          "Vidal",
          "Serrano",
          "Herrera",
          "Campos",
          "Iglesias",
          "Santos",
          "Peña",
          "Cortés",
          "Medina",
          "Lozano",
          "Prieto",
          "Marín",
          "Gil",
          "Vega");
}
