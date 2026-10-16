package com.fleetcontrol.msdrivers.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class holding the business rules and limits of the driver service. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessConstants {

  /** Lowest number of days accepted by the license expiration filter. */
  public static final int MIN_EXPIRING_DAYS = 0;

  /** Pagination limits shared by every list endpoint. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Pagination {
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MIN_PAGE = 0;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
  }

  /** Maximum length of the text fields of a driver. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class FieldLimits {
    public static final int FULL_NAME_MAX = 255;
    public static final int EMAIL_MAX = 255;
    public static final int PHONE_MAX = 30;
    public static final int LICENSE_NUMBER_MAX = 50;
  }

  /** Names of the entity attributes used to filter and sort. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Fields {
    public static final String ID = "id";
    public static final String FULL_NAME = "fullName";
    public static final String STATUS = "status";
    public static final String LICENSE_EXPIRES_AT = "licenseExpiresAt";
  }
}
