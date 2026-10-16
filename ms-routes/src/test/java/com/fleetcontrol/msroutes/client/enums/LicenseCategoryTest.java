package com.fleetcontrol.msroutes.client.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for the license category / vehicle type compatibility matrix. */
class LicenseCategoryTest {

  @ParameterizedTest(name = "{0} allows {1}: {2}")
  @CsvSource({
    "A, MOTORCYCLE, true",
    "A, CAR, false",
    "A, VAN, false",
    "A, TRUCK, false",
    "B, CAR, true",
    "B, VAN, true",
    "B, TRUCK, false",
    "B, MOTORCYCLE, false",
    "C, CAR, true",
    "C, VAN, true",
    "C, TRUCK, true",
    "C, MOTORCYCLE, false"
  })
  void allows_followsContractMatrix(LicenseCategory category, VehicleType type, boolean expected) {
    assertThat(category.allows(type)).isEqualTo(expected);
  }
}
