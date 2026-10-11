package com.fleetcontrol.msgateway.config;

import com.fleetcontrol.msgateway.config.properties.ServiceProperties;
import com.fleetcontrol.msgateway.constants.RouteConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Defines the routes that forward each API path to its microservice. */
@Configuration
@RequiredArgsConstructor
public class RouteConfig {
  private final ServiceProperties serviceProperties;

  /** Builds one route per microservice from the configured service URLs. */
  @Bean
  public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder
        .routes()
        .route(
            RouteConstants.Id.AUTH,
            r -> r.path(RouteConstants.GatewayRoute.AUTH).uri(serviceProperties.auth()))
        .route(
            RouteConstants.Id.ROUTES,
            r -> r.path(RouteConstants.GatewayRoute.ROUTES).uri(serviceProperties.routes()))
        .route(
            RouteConstants.Id.VEHICLES,
            r -> r.path(RouteConstants.GatewayRoute.VEHICLES).uri(serviceProperties.vehicles()))
        .route(
            RouteConstants.Id.FUEL,
            r -> r.path(RouteConstants.GatewayRoute.FUEL).uri(serviceProperties.fuel()))
        .route(
            RouteConstants.Id.DRIVERS,
            r -> r.path(RouteConstants.GatewayRoute.DRIVERS).uri(serviceProperties.drivers()))
        .route(
            RouteConstants.Id.MAINTENANCE,
            r ->
                r.path(RouteConstants.GatewayRoute.MAINTENANCE)
                    .uri(serviceProperties.maintenance()))
        .route(
            RouteConstants.Id.ALERTS,
            r -> r.path(RouteConstants.GatewayRoute.ALERTS).uri(serviceProperties.alerts()))
        .route(
            RouteConstants.Id.DASHBOARD,
            r -> r.path(RouteConstants.GatewayRoute.DASHBOARD).uri(serviceProperties.dashboard()))
        .build();
  }
}
