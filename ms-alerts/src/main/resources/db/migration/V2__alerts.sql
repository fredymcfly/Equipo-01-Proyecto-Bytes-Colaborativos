-- Alerts raised by the evaluator and rules that configure it.

CREATE TABLE alerts (
    id              UUID PRIMARY KEY,
    type            VARCHAR(30)  NOT NULL,
    severity        VARCHAR(10)  NOT NULL,
    status          VARCHAR(15)  NOT NULL,
    vehicle_id      UUID,
    driver_id       UUID,
    message         VARCHAR(500) NOT NULL,
    dedup_key       VARCHAR(120) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by UUID,
    comment         VARCHAR(500),
    resolved_at     TIMESTAMPTZ
);

-- A condition has at most one active alert. Once it is resolved and the condition appears
-- again, a new alert is created, so the key is unique only while the alert is not resolved.
CREATE UNIQUE INDEX uk_alerts_dedup_key_active
    ON alerts (dedup_key) WHERE status <> 'RESOLVED';

CREATE INDEX idx_alerts_status ON alerts (status);
CREATE INDEX idx_alerts_vehicle_id ON alerts (vehicle_id);
CREATE INDEX idx_alerts_created_at ON alerts (created_at);

CREATE TABLE alert_rules (
    type       VARCHAR(30) PRIMARY KEY,
    enabled    BOOLEAN     NOT NULL,
    severity   VARCHAR(10) NOT NULL,
    params     JSONB       NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO alert_rules (type, enabled, severity, params, updated_at) VALUES
    ('MAINTENANCE_DUE', TRUE, 'WARNING', '{"daysBefore": 7}', NOW()),
    ('MAINTENANCE_OVERDUE', TRUE, 'CRITICAL', '{}', NOW()),
    ('LICENSE_EXPIRING', TRUE, 'WARNING', '{"daysBefore": 30}', NOW()),
    ('LICENSE_EXPIRED', TRUE, 'CRITICAL', '{}', NOW()),
    ('HIGH_CONSUMPTION', TRUE, 'WARNING', '{"thresholdPercent": 25}', NOW());
