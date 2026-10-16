package com.fleetcontrol.msdrivers.repository;

import com.fleetcontrol.msdrivers.constants.BusinessConstants;
import com.fleetcontrol.msdrivers.models.entities.Driver;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** Reusable filters for driver queries. */
public final class DriverSpecifications {

  private DriverSpecifications() {}

  /** Matches drivers with the given status, or all of them when it is null. */
  public static Specification<Driver> hasStatus(DriverStatus status) {
    return (root, query, builder) ->
        status == null
            ? builder.conjunction()
            : builder.equal(root.get(BusinessConstants.Fields.STATUS), status);
  }

  /**
   * Matches drivers whose license expires on the given date or before, which includes the licenses
   * that are already expired. When the date is null it matches all of them.
   */
  public static Specification<Driver> licenseExpiresOnOrBefore(LocalDate limit) {
    return (root, query, builder) ->
        limit == null
            ? builder.conjunction()
            : builder.lessThanOrEqualTo(
                root.get(BusinessConstants.Fields.LICENSE_EXPIRES_AT), limit);
  }
}
