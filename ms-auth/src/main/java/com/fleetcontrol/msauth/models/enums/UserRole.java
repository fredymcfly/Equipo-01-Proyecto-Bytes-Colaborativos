package com.fleetcontrol.msauth.models.enums;

/** Roles a user can have in the FleetControl platform. */
public enum UserRole {
  /** Default role assigned at registration. */
  MANAGER,
  /** Includes everything a manager can do plus alert rules and manual evaluations. */
  ADMIN
}
