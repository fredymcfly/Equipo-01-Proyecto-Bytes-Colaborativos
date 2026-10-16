package com.fleetcontrol.msdrivers.service;

import com.fleetcontrol.msdrivers.constants.BusinessConstants;
import com.fleetcontrol.msdrivers.exception.DriverAlreadyExistsException;
import com.fleetcontrol.msdrivers.exception.DriverNotFoundException;
import com.fleetcontrol.msdrivers.mapper.DriverMapper;
import com.fleetcontrol.msdrivers.models.dto.request.CreateDriverRequest;
import com.fleetcontrol.msdrivers.models.dto.request.UpdateDriverRequest;
import com.fleetcontrol.msdrivers.models.dto.response.DriverResponse;
import com.fleetcontrol.msdrivers.models.dto.response.DriverSummaryResponse;
import com.fleetcontrol.msdrivers.models.dto.response.PageResponse;
import com.fleetcontrol.msdrivers.models.entities.Driver;
import com.fleetcontrol.msdrivers.models.enums.DriverStatus;
import com.fleetcontrol.msdrivers.repository.DriverRepository;
import com.fleetcontrol.msdrivers.repository.DriverSpecifications;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for the driver catalogue. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DriverService {

  private final DriverRepository repository;
  private final DriverMapper mapper;
  private final Clock clock;

  /** Registers a new driver, which always starts as active. */
  @Transactional
  public DriverResponse create(CreateDriverRequest request) {
    if (repository.existsByLicenseNumber(request.licenseNumber())) {
      throw new DriverAlreadyExistsException();
    }
    Driver driver = mapper.toEntity(request);
    driver.setStatus(DriverStatus.ACTIVE);
    return mapper.toResponse(repository.saveAndFlush(driver));
  }

  /**
   * Returns a page of drivers, optionally filtered by status and by the days left until the license
   * expires. The licenses that are already expired are included in the second filter.
   */
  public PageResponse<DriverSummaryResponse> list(
      DriverStatus status, Integer licenseExpiringInDays, int page, int size) {
    LocalDate limit =
        licenseExpiringInDays == null
            ? null
            : LocalDate.now(clock).plusDays(licenseExpiringInDays.longValue());
    Specification<Driver> filters =
        DriverSpecifications.hasStatus(status)
            .and(DriverSpecifications.licenseExpiresOnOrBefore(limit));
    Sort sort = Sort.by(BusinessConstants.Fields.FULL_NAME, BusinessConstants.Fields.ID);
    Page<DriverSummaryResponse> result =
        repository.findAll(filters, PageRequest.of(page, size, sort)).map(mapper::toSummary);
    return PageResponse.from(result);
  }

  /** Returns the detailed data of one driver. */
  public DriverResponse getById(UUID driverId) {
    return mapper.toResponse(findDriver(driverId));
  }

  /** Updates every data of a driver, including the status and the license, which can be renewed. */
  @Transactional
  public DriverResponse update(UUID driverId, UpdateDriverRequest request) {
    Driver driver = findDriver(driverId);
    if (repository.existsByLicenseNumberAndIdNot(request.licenseNumber(), driverId)) {
      throw new DriverAlreadyExistsException();
    }
    mapper.applyUpdate(request, driver);
    return mapper.toResponse(repository.saveAndFlush(driver));
  }

  private Driver findDriver(UUID driverId) {
    return repository.findById(driverId).orElseThrow(DriverNotFoundException::new);
  }
}
