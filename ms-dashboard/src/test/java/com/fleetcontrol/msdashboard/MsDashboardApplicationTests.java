package com.fleetcontrol.msdashboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** The application starts with its real configuration and its Feign clients. */
@SpringBootTest
@ActiveProfiles("test")
class MsDashboardApplicationTests {

  @Test
  void contextLoads() {}
}
