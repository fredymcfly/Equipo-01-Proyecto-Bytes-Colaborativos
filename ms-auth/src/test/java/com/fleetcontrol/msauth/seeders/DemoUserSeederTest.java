package com.fleetcontrol.msauth.seeders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msauth.config.properties.DemoProperties;
import com.fleetcontrol.msauth.models.entities.User;
import com.fleetcontrol.msauth.models.enums.UserRole;
import com.fleetcontrol.msauth.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DemoUserSeederTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private DemoUserSeeder seeder(String password, int seed) {
    DemoProperties demo = new DemoProperties(seed, 90, "Admin@FleetControl.com", password);
    return new DemoUserSeeder(userRepository, passwordEncoder, demo);
  }

  private User run(DemoUserSeeder seeder) {
    seeder.run(null);
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void createsTheAdminWhenTheTableIsEmpty() {
    when(userRepository.count()).thenReturn(0L);
    when(passwordEncoder.encode("secret-123")).thenReturn("hash");

    User admin = run(seeder("secret-123", 42));

    assertThat(admin.getEmail()).isEqualTo("admin@fleetcontrol.com");
    assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(admin.getPassword()).isEqualTo("hash");
    assertThat(admin.getId()).isNotNull();
  }

  @Test
  void theIdIsDeterministicForTheSameSeed() {
    when(userRepository.count()).thenReturn(0L);
    when(passwordEncoder.encode("secret-123")).thenReturn("hash");
    User first = run(seeder("secret-123", 42));

    org.mockito.Mockito.clearInvocations(userRepository);
    User second = run(seeder("secret-123", 42));

    assertThat(second.getId()).isEqualTo(first.getId());
  }

  @Test
  void doesNothingWhenTheTableIsNotEmpty() {
    when(userRepository.count()).thenReturn(3L);

    seeder("secret-123", 42).run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void failsClearlyWhenThePasswordIsBlank() {
    when(userRepository.count()).thenReturn(0L);

    assertThatThrownBy(() -> seeder("  ", 42).run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEMO_ADMIN_PASSWORD");
    verify(userRepository, never()).save(any());
  }
}
