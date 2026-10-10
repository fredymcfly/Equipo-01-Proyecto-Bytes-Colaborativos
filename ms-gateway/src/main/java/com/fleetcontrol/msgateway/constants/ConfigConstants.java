package com.fleetcontrol.msgateway.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Centralized configuration constants and property prefixes for the application. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigConstants {
  public static final String BASE_PACKAGE = "com.fleetcontrol.msgateway";

  /** Configuration property prefixes used for binding external properties. */
  public static final class Properties {
    public static final String FLEET_GATEWAY_SERVICES = "fleet.gateway.service";
    public static final String FLEET_RATE_LIMIT = "fleet.gateway.rate-limit";
  }
}
