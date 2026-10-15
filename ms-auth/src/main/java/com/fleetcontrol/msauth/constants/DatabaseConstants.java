package com.fleetcontrol.msauth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Names of database objects that the code needs to recognize. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatabaseConstants {

  /** Constraint names defined in the Flyway migrations. */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Constraint {
    public static final String USERS_USERNAME = "uk_users_username";
    public static final String USERS_EMAIL = "uk_users_email";
  }
}
