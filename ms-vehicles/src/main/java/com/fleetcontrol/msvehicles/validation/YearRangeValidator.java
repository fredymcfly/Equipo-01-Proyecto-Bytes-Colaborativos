package com.fleetcontrol.msvehicles.validation;

import com.fleetcontrol.msvehicles.constants.BusinessConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

/** Checks the year range accepted by {@link ValidYear}. */
public class YearRangeValidator implements ConstraintValidator<ValidYear, Integer> {

  @Override
  public boolean isValid(Integer year, ConstraintValidatorContext context) {
    if (year == null) {
      return true;
    }
    return year >= BusinessConstants.MIN_YEAR
        && year <= Year.now().getValue() + BusinessConstants.NEXT_YEARS_ALLOWED;
  }
}
