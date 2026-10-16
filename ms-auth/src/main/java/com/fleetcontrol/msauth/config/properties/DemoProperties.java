package com.fleetcontrol.msauth.config.properties;

import com.fleetcontrol.msauth.constants.ConfigConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the demo data seeder (profile {@code demo}).
 *
 * @param seed seed used to derive deterministic UUIDs
 * @param days number of days of history to generate
 * @param adminEmail email of the admin user created by the seeder
 * @param adminPassword password of the admin user; the seeder must not run if it is blank
 */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_DEMO)
public record DemoProperties(int seed, int days, String adminEmail, String adminPassword) {

  @Override
  public String toString() {
    return "DemoProperties[seed="
        + seed
        + ", days="
        + days
        + ", adminEmail="
        + adminEmail
        + ", adminPassword=***]";
  }
}
