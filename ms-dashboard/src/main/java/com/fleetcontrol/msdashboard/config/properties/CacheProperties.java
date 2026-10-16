package com.fleetcontrol.msdashboard.config.properties;

import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the response cache.
 *
 * @param ttl time an answer stays in the cache
 * @param maxEntries maximum number of answers kept per cache
 */
@ConfigurationProperties(prefix = ConfigConstants.Properties.FLEET_CACHE)
public record CacheProperties(Duration ttl, long maxEntries) {}
