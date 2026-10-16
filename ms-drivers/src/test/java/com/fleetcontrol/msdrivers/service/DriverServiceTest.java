package com.fleetcontrol.msdrivers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.fleetcontrol.msdrivers.models.enums.LicenseCategory;
import com.fleetcontrol.msdrivers.repository.DriverRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** Unit tests of the business rules of the driver service. */
@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-05T10:30:00Z"), ZoneOffset.UTC);
  private static final String LICENSE = "B-5120377";
  private static final LocalDate FUTURE = LocalDate.of(2029, 6, 30);

  @Mock private DriverRepository repository;
  @Mock private DriverMapper mapper;

  private DriverService service;

  @BeforeEach
  void setUp() {
    service = new DriverService(repository, mapper, CLOCK);
  }

  @Test
  void create_savesTheDriverAsActive() {
    CreateDriverRequest request = createRequest();
    Driver entity = new Driver();
    entity.setStatus(DriverStatus.SUSPENDED);
    DriverResponse response = response(UUID.randomUUID());
    when(repository.existsByLicenseNumber(LICENSE)).thenReturn(false);
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.saveAndFlush(entity)).thenReturn(entity);
    when(mapper.toResponse(entity)).thenReturn(response);

    DriverResponse result = service.create(request);

    assertThat(result).isSameAs(response);
    assertThat(entity.getStatus()).isEqualTo(DriverStatus.ACTIVE);
  }

  @Test
  void create_withExistingLicense_throwsAndSavesNothing() {
    when(repository.existsByLicenseNumber(LICENSE)).thenReturn(true);

    assertThatThrownBy(() -> service.create(createRequest()))
        .isInstanceOf(DriverAlreadyExistsException.class);

    verify(repository, never()).saveAndFlush(any(Driver.class));
    verifyNoInteractions(mapper);
  }

  @Test
  void getById_returnsTheDriver() {
    UUID id = UUID.randomUUID();
    Driver driver = new Driver();
    DriverResponse response = response(id);
    when(repository.findById(id)).thenReturn(Optional.of(driver));
    when(mapper.toResponse(driver)).thenReturn(response);

    assertThat(service.getById(id)).isSameAs(response);
  }

  @Test
  void getById_ofAnUnknownDriver_throws() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(id)).isInstanceOf(DriverNotFoundException.class);
  }

  @Test
  void update_appliesTheChangesAndFlushesThem() {
    UUID id = UUID.randomUUID();
    Driver driver = new Driver();
    UpdateDriverRequest request = updateRequest();
    DriverResponse response = response(id);
    when(repository.findById(id)).thenReturn(Optional.of(driver));
    when(repository.existsByLicenseNumberAndIdNot(LICENSE, id)).thenReturn(false);
    when(repository.saveAndFlush(driver)).thenReturn(driver);
    when(mapper.toResponse(driver)).thenReturn(response);

    DriverResponse result = service.update(id, request);

    assertThat(result).isSameAs(response);
    verify(mapper).applyUpdate(request, driver);
  }

  @Test
  void update_withTheLicenseOfAnotherDriver_throwsAndChangesNothing() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.of(new Driver()));
    when(repository.existsByLicenseNumberAndIdNot(LICENSE, id)).thenReturn(true);

    assertThatThrownBy(() -> service.update(id, updateRequest()))
        .isInstanceOf(DriverAlreadyExistsException.class);

    verify(mapper, never()).applyUpdate(any(UpdateDriverRequest.class), any(Driver.class));
    verify(repository, never()).saveAndFlush(any(Driver.class));
  }

  @Test
  void update_ofAnUnknownDriver_throws() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(id, updateRequest()))
        .isInstanceOf(DriverNotFoundException.class);
  }

  @Test
  void list_returnsThePageSortedByName() {
    Driver driver = new Driver();
    DriverSummaryResponse summary =
        new DriverSummaryResponse(
            UUID.randomUUID(),
            "Ana Ruiz",
            "ana@fleetcontrol.com",
            null,
            LICENSE,
            LicenseCategory.B,
            FUTURE,
            DriverStatus.ACTIVE);
    when(repository.findAll(ArgumentMatchers.<Specification<Driver>>any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(driver), PageRequest.of(0, 20), 1));
    when(mapper.toSummary(driver)).thenReturn(summary);

    PageResponse<DriverSummaryResponse> result = service.list(DriverStatus.ACTIVE, 30, 0, 20);

    assertThat(result.content()).containsExactly(summary);
    assertThat(result.totalElements()).isEqualTo(1);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(ArgumentMatchers.<Specification<Driver>>any(), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("fullName", "id"));
  }

  private static CreateDriverRequest createRequest() {
    return new CreateDriverRequest(
        "Sara Ibáñez",
        "sara.ibanez@fleetcontrol.com",
        "+34 622 456 789",
        LICENSE,
        LicenseCategory.B,
        FUTURE);
  }

  private static UpdateDriverRequest updateRequest() {
    return new UpdateDriverRequest(
        "Sara Ibáñez",
        "sara.ibanez@fleetcontrol.com",
        "+34 622 456 789",
        LICENSE,
        LicenseCategory.B,
        FUTURE,
        DriverStatus.ON_LEAVE);
  }

  private static DriverResponse response(UUID id) {
    return new DriverResponse(
        id,
        "Sara Ibáñez",
        "sara.ibanez@fleetcontrol.com",
        "+34 622 456 789",
        LICENSE,
        LicenseCategory.B,
        FUTURE,
        DriverStatus.ACTIVE,
        CLOCK.instant(),
        CLOCK.instant());
  }
}
