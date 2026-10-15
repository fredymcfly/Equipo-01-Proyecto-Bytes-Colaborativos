package com.fleetcontrol.msauth;

import com.fleetcontrol.msauth.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MsAuthApplicationTests {

  @Test
  void contextLoads() {}
}
