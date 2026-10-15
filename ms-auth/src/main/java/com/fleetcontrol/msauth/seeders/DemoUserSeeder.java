package com.fleetcontrol.msauth.seeders;

import com.fleetcontrol.msauth.config.properties.DemoProperties;
import com.fleetcontrol.msauth.constants.ConfigConstants;
import com.fleetcontrol.msauth.models.entities.User;
import com.fleetcontrol.msauth.models.enums.UserRole;
import com.fleetcontrol.msauth.repositories.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code ADMIN} user when the {@code demo} profile is active.
 *
 * <p>It is idempotent: it only inserts when the {@code users} table is empty. The id is derived
 * from {@code DEMO_SEED}, so the same seed always produces the same UUID.
 */
@Slf4j
@Component
@Profile(ConfigConstants.Profiles.DEMO)
@RequiredArgsConstructor
public class DemoUserSeeder implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final DemoProperties demo;

  /**
   * Seeds the admin user.
   *
   * @param args the application arguments, not used
   * @throws IllegalStateException if the table is empty and {@code DEMO_ADMIN_PASSWORD} is blank
   */
  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.count() > 0) {
      log.info("Demo seeder skipped: the users table is not empty");
      return;
    }
    if (demo.adminPassword() == null || demo.adminPassword().isBlank()) {
      throw new IllegalStateException(
          "DEMO_ADMIN_PASSWORD must be set when the 'demo' profile is active");
    }

    String email = demo.adminEmail().trim().toLowerCase(Locale.ROOT);
    userRepository.save(
        User.builder()
            .id(adminId())
            .username(ConfigConstants.Demo.ADMIN_USERNAME)
            .email(email)
            .password(passwordEncoder.encode(demo.adminPassword()))
            .role(UserRole.ADMIN)
            .build());
    log.info("Demo seeder created the ADMIN user {}", email);
  }

  private UUID adminId() {
    String name = ConfigConstants.Demo.ADMIN_UUID_PREFIX + demo.seed();
    return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
  }
}
