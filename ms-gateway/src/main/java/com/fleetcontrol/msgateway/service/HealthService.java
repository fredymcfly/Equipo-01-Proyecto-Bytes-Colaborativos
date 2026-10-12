package com.fleetcontrol.msgateway.service;

import com.fleetcontrol.msgateway.config.properties.ServiceProperties;
import com.fleetcontrol.msgateway.constants.HealthConstants;
import com.fleetcontrol.msgateway.dto.HealthResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Checks the {@code /actuator/health} of every downstream service in parallel. A service that
 * answers with an error or does not answer within the timeout is reported as {@code DOWN}.
 */
@Service
public class HealthService {

  private static final Duration SERVICE_TIMEOUT = Duration.ofSeconds(2);

  private final ServiceProperties services;
  private final WebClient webClient;
  private final Duration timeout;

  /** Creates the service with the 2 seconds timeout of the gateway contract. */
  @Autowired
  public HealthService(ServiceProperties services, WebClient.Builder webClientBuilder) {
    this(services, webClientBuilder.build(), SERVICE_TIMEOUT);
  }

  HealthService(ServiceProperties services, WebClient webClient, Duration timeout) {
    this.services = services;
    this.webClient = webClient;
    this.timeout = timeout;
  }

  /** Returns the status of the gateway and its services; it never fails. */
  public Mono<HealthResponse> check() {
    return Flux.fromIterable(serviceUrls().entrySet())
        .flatMapSequential(
            entry -> statusOf(entry.getValue()).map(status -> Map.entry(entry.getKey(), status)))
        .collectList()
        .map(
            results -> {
              Map<String, String> statuses = new LinkedHashMap<>();
              results.forEach(result -> statuses.put(result.getKey(), result.getValue()));
              String timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
              return new HealthResponse(HealthConstants.UP, timestamp, statuses);
            });
  }

  private Mono<String> statusOf(String baseUrl) {
    if (!StringUtils.hasText(baseUrl)) {
      return Mono.just(HealthConstants.DOWN);
    }
    return webClient
        .get()
        .uri(baseUrl.replaceAll("/+$", "") + HealthConstants.ACTUATOR_HEALTH_PATH)
        .retrieve()
        .toBodilessEntity()
        .timeout(timeout)
        .map(response -> HealthConstants.UP)
        .onErrorReturn(HealthConstants.DOWN);
  }

  private Map<String, String> serviceUrls() {
    Map<String, String> urls = new LinkedHashMap<>();
    urls.put(HealthConstants.ServiceName.AUTH, services.auth());
    urls.put(HealthConstants.ServiceName.VEHICLES, services.vehicles());
    urls.put(HealthConstants.ServiceName.DRIVERS, services.drivers());
    urls.put(HealthConstants.ServiceName.ROUTES, services.routes());
    urls.put(HealthConstants.ServiceName.MAINTENANCE, services.maintenance());
    urls.put(HealthConstants.ServiceName.FUEL, services.fuel());
    urls.put(HealthConstants.ServiceName.ALERTS, services.alerts());
    urls.put(HealthConstants.ServiceName.DASHBOARD, services.dashboard());
    return urls;
  }
}
