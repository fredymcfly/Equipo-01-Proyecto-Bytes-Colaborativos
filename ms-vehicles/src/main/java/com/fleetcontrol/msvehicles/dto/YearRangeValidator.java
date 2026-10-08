package com.fleetcontrol.msvehicles.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

/** Checks the year range accepted by {@link ValidYear}. */
public class YearRangeValidator implements ConstraintValidator<ValidYear, Integer> {

  private static final int MIN_YEAR = 1990;

  @Override
  public boolean isValid(Integer year, ConstraintValidatorContext context) {
    if (year == null) {
      return true;
    }
    return year >= MIN_YEAR && year <= Year.now().getValue() + 1;
  }
}
