package com.fleetcontrol.msalerts.repository;

import com.fleetcontrol.msalerts.models.entities.AlertRule;
import com.fleetcontrol.msalerts.models.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the configuration of the alert rules. */
public interface AlertRuleRepository extends JpaRepository<AlertRule, AlertType> {}
