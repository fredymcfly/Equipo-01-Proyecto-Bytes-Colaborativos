package com.fleetcontrol.msalerts.config;

import com.fleetcontrol.msalerts.constants.ConfigConstants;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/** Enables the Feign clients of the source services. */
@Configuration
@EnableFeignClients(basePackages = ConfigConstants.CLIENT_PACKAGE)
public class FeignConfig {}
