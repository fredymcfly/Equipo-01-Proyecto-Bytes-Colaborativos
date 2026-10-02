package com.fleetcontrol.msgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the ms-gateway service. */
@SpringBootApplication
public class MsGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsGatewayApplication.class, args);
  }
}
