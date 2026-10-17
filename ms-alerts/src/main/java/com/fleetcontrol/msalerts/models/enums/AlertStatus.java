package com.fleetcontrol.msalerts.models.enums;

/** Life cycle of an alert. */
public enum AlertStatus {
  /** Detected and not seen yet. */
  OPEN,
  /** Seen by a person. It stays active until the condition disappears. */
  ACKNOWLEDGED,
  /** The condition that raised it no longer holds. */
  RESOLVED
}
