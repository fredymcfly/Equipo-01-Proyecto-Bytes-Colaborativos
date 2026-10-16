package com.fleetcontrol.msgateway.constants;

import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Names of the HTTP headers the gateway reads, adds or removes. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeaderConstants {
  public static final String X_REQUEST_ID = "X-Request-Id";
  public static final String X_INTERNAL_KEY = "X-Internal-Key";

  /** Request ids the gateway accepts from clients: 1 to 64 letters, digits, dots, _ or -. */
  public static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
}
