package com.fleetcontrol.msdashboard.config;

import com.fleetcontrol.msdashboard.constants.ConfigConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Thread pool used to call the source services in parallel. */
@Configuration
public class ExecutorConfig {

  /** Pool shared by every request. Its size bounds the calls running at the same time. */
  @Bean(name = ConfigConstants.Executor.BEAN_NAME)
  public ThreadPoolTaskExecutor sourceExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(ConfigConstants.Executor.THREADS);
    executor.setMaxPoolSize(ConfigConstants.Executor.THREADS);
    executor.setQueueCapacity(ConfigConstants.Executor.QUEUE_CAPACITY);
    executor.setThreadNamePrefix(ConfigConstants.Executor.THREAD_PREFIX);
    return executor;
  }
}
