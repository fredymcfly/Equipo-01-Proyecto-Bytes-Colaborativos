package com.fleetcontrol.msauth.repositories;

import com.fleetcontrol.msauth.models.entities.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence access for {@link User}. Emails are stored and searched in lowercase. */
public interface UserRepository extends JpaRepository<User, UUID> {

  /** Finds a user by its (lowercase) email, used by the login. */
  Optional<User> findByEmail(String email);

  /** Tells whether a user with this (lowercase) email already exists. */
  boolean existsByEmail(String email);

  /** Tells whether a user with this exact username already exists. */
  boolean existsByUsername(String username);
}
