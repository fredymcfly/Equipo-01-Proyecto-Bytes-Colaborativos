package com.fleetcontrol.msdashboard.config;

import com.fleetcontrol.msdashboard.config.properties.CacheProperties;
import com.fleetcontrol.msdashboard.constants.BusinessConstants;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Caffeine caches that keep each answer for the configured time, 60 seconds by default. */
@Configuration
@EnableCaching
public class CacheConfig {

  /** Cache manager with one cache per endpoint. The key of each entry is the query. */
  @Bean
  public CacheManager cacheManager(CacheProperties properties) {
    CaffeineCacheManager manager =
        new CaffeineCacheManager(
            BusinessConstants.Cache.OVERVIEW,
            BusinessConstants.Cache.TIMESERIES,
            BusinessConstants.Cache.RANKING);
    manager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(properties.ttl())
            .maximumSize(properties.maxEntries()));
    return manager;
  }
}
