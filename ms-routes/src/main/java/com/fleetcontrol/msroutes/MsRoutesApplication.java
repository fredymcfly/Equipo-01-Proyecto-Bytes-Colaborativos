package com.fleetcontrol.msroutes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Entry point of the ms-routes service. */
@EnableFeignClients
@SpringBootApplication
public class MsRoutesApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsRoutesApplication.class, args);
  }
}
