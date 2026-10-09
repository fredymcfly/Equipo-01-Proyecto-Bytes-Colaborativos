package com.fleetcontrol.msroutes.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/** Enables the Feign clients of ms-routes. */
@Configuration
@EnableFeignClients(basePackages = "com.fleetcontrol.msroutes.client")
public class FeignEnableConfig {}
