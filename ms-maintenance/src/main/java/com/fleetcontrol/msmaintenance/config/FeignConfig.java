package com.fleetcontrol.msmaintenance.config;

import com.fleetcontrol.msmaintenance.client.VehicleClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/** Enables the Feign clients used to call other services. */
@Configuration
@EnableFeignClients(clients = VehicleClient.class)
public class FeignConfig {}
