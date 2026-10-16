package com.fleetcontrol.msdashboard.service;

import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import feign.FeignException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Runs the calls to the source services in parallel. A call that fails, whether the service is
 * down, is too slow or answers with an error, does not break the request: it is reported as an
 * unavailable {@link SourceResult} so that the caller can return what it has.
 */
@Slf4j
@Component
public class SourceCaller {

  private final Executor executor;

  /** Creates the caller with the pool that runs the calls. */
  public SourceCaller(@Qualifier(ConfigConstants.Executor.BEAN_NAME) Executor executor) {
    this.executor = executor;
  }

  /** Starts a call to the given service without waiting for its answer. */
  public <T> CompletableFuture<SourceResult<T>> call(String service, Supplier<T> request) {
    return CompletableFuture.supplyAsync(() -> execute(service, request), executor);
  }

  private <T> SourceResult<T> execute(String service, Supplier<T> request) {
    try {
      return new SourceResult<>(service, request.get());
    } catch (FeignException exception) {
      log.warn("{} did not answer: {}", service, exception.getMessage());
      return new SourceResult<>(service, null);
    }
  }
}
