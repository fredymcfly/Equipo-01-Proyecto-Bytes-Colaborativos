package com.fleetcontrol.msvehicles.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates that a vehicle year is between 1990 and the next year. */
@Documented
@Constraint(validatedBy = YearRangeValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidYear {

  /** Default violation message, in English like the rest of the contract. */
  String message() default "must be between 1990 and next year";

  /** Validation groups this constraint belongs to. */
  Class<?>[] groups() default {};

  /** Validation payload, used by clients to attach metadata. */
  Class<? extends Payload>[] payload() default {};
}
