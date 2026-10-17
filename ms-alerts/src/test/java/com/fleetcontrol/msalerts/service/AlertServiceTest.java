package com.fleetcontrol.msalerts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msalerts.exception.AlertNotFoundException;
import com.fleetcontrol.msalerts.exception.InvalidAlertStateException;
import com.fleetcontrol.msalerts.models.dto.request.AcknowledgeRequest;
import com.fleetcontrol.msalerts.models.dto.response.AlertResponse;
import com.fleetcontrol.msalerts.models.dto.response.PageResponse;
import com.fleetcontrol.msalerts.models.entities.Alert;
import com.fleetcontrol.msalerts.models.enums.AlertSeverity;
import com.fleetcontrol.msalerts.models.enums.AlertStatus;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** Unit tests of the business rules of the alert service. */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-05T10:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private AlertRepository repository;

  private AlertService service;

  @BeforeEach
  void setUp() {
    service = new AlertService(repository, CLOCK);
  }

  @Test
  void acknowledge_marksTheOpenAlertWithUserDateAndComment() {
    Alert alert = alert(AlertStatus.OPEN);
    UUID userId = UUID.randomUUID();
    when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
    when(repository.saveAndFlush(alert)).thenReturn(alert);

    AlertResponse response =
        service.acknowledge(
            alert.getId(), new AcknowledgeRequest("Cita pedida"), userId.toString());

    assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(response.acknowledgedAt()).isEqualTo(NOW);
    assertThat(response.acknowledgedBy()).isEqualTo(userId);
    assertThat(response.comment()).isEqualTo("Cita pedida");
  }

  @Test
  void acknowledge_withoutRequest_keepsNoComment() {
    Alert alert = alert(AlertStatus.OPEN);
    when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
    when(repository.saveAndFlush(alert)).thenReturn(alert);

    AlertResponse response = service.acknowledge(alert.getId(), null, UUID.randomUUID().toString());

    assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(response.comment()).isNull();
  }

  @Test
  void acknowledge_withAnActorThatIsNotAnId_storesNoUser() {
    Alert alert = alert(AlertStatus.OPEN);
    when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));
    when(repository.saveAndFlush(alert)).thenReturn(alert);

    AlertResponse response = service.acknowledge(alert.getId(), null, "tester");

    assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(response.acknowledgedBy()).isNull();
  }

  @Test
  void acknowledge_unknownAlert_isNotFound() {
    UUID alertId = UUID.randomUUID();
    when(repository.findById(alertId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.acknowledge(alertId, null, "tester"))
        .isInstanceOf(AlertNotFoundException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @ParameterizedTest
  @EnumSource(
      value = AlertStatus.class,
      names = {"ACKNOWLEDGED", "RESOLVED"})
  void acknowledge_alertThatIsNotOpen_isRejected(AlertStatus status) {
    Alert alert = alert(status);
    when(repository.findById(alert.getId())).thenReturn(Optional.of(alert));

    assertThatThrownBy(() -> service.acknowledge(alert.getId(), null, "tester"))
        .isInstanceOf(InvalidAlertStateException.class);
    assertThat(alert.getStatus()).isEqualTo(status);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void list_returnsThePageWithoutAnExtraSort() {
    Alert alert = alert(AlertStatus.OPEN);
    when(repository.findAll(anySpecification(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(alert), PageRequest.of(1, 5), 6));

    PageResponse<AlertResponse> page = service.list(null, null, null, null, 1, 5);

    assertThat(page.content()).extracting(AlertResponse::id).containsExactly(alert.getId());
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(5);
    assertThat(page.totalElements()).isEqualTo(6);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(anySpecification(), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    assertThat(pageable.getValue().getSort().isSorted()).isFalse();
  }

  private static Specification<Alert> anySpecification() {
    return any();
  }

  private static Alert alert(AlertStatus status) {
    Alert alert = new Alert();
    alert.setId(UUID.randomUUID());
    alert.setType(AlertType.MAINTENANCE_OVERDUE);
    alert.setSeverity(AlertSeverity.CRITICAL);
    alert.setStatus(status);
    alert.setMessage("Alert");
    alert.setDedupKey("key");
    alert.setCreatedAt(NOW.minusSeconds(3600));
    return alert;
  }
}
