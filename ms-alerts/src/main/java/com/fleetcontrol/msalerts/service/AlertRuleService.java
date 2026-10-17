package com.fleetcontrol.msalerts.service;

import com.fleetcontrol.msalerts.exception.RuleNotFoundException;
import com.fleetcontrol.msalerts.models.dto.request.UpdateRuleRequest;
import com.fleetcontrol.msalerts.models.dto.response.RuleResponse;
import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import com.fleetcontrol.msalerts.repository.AlertRuleRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for the configuration of the alert rules. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AlertRuleService {

  private final AlertRuleRepository repository;
  private final RuleParamsValidator validator;

  /** Lists the rules in the order of their types. */
  public List<RuleResponse> list() {
    return repository.findAll().stream()
        .sorted(Comparator.comparing(AlertRule::getType))
        .map(RuleResponse::from)
        .toList();
  }

  /**
   * Replaces the configuration of a rule. The type comes as text, so a value that is not a type is
   * answered like a rule that does not exist. The change applies from the next evaluation.
   */
  @Transactional
  public RuleResponse update(String rawType, UpdateRuleRequest request) {
    AlertType type = parseType(rawType);
    AlertRule rule = repository.findById(type).orElseThrow(RuleNotFoundException::new);
    validator.validate(type, request.params());
    rule.setEnabled(request.enabled());
    rule.setSeverity(request.severity());
    rule.setParams(request.params() == null ? new HashMap<>() : new HashMap<>(request.params()));
    return RuleResponse.from(repository.saveAndFlush(rule));
  }

  private AlertType parseType(String rawType) {
    try {
      return AlertType.valueOf(rawType);
    } catch (IllegalArgumentException exception) {
      throw new RuleNotFoundException();
    }
  }
}
