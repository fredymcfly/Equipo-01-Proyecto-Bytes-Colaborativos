CREATE TABLE maintenance_plans (
  id UUID PRIMARY KEY,
  vehicle_id UUID NOT NULL,
  type VARCHAR(30) NOT NULL,
  interval_km INTEGER,
  interval_days INTEGER,
  last_done_at DATE NOT NULL,
  last_done_km INTEGER NOT NULL,
  next_due_at DATE,
  next_due_km INTEGER,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT ck_plans_has_interval CHECK (interval_km IS NOT NULL OR interval_days IS NOT NULL),
  CONSTRAINT ck_plans_interval_km CHECK (interval_km IS NULL OR interval_km > 0),
  CONSTRAINT ck_plans_interval_days CHECK (interval_days IS NULL OR interval_days > 0)
);

-- A vehicle can only have one active plan per maintenance type.
CREATE UNIQUE INDEX uq_plans_active_vehicle_type ON maintenance_plans (vehicle_id, type) WHERE active;
CREATE INDEX idx_plans_next_due_at ON maintenance_plans (next_due_at);

CREATE TABLE maintenance_orders (
  id UUID PRIMARY KEY,
  plan_id UUID NOT NULL REFERENCES maintenance_plans (id),
  vehicle_id UUID NOT NULL,
  type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  scheduled_for DATE NOT NULL,
  started_at TIMESTAMP WITH TIME ZONE,
  completed_at TIMESTAMP WITH TIME ZONE,
  odometer_km INTEGER,
  cost NUMERIC(10, 2),
  workshop VARCHAR(150),
  notes VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT ck_orders_cost CHECK (cost IS NULL OR cost >= 0)
);

-- A plan can only have one open order (PENDING or IN_PROGRESS) at a time.
CREATE UNIQUE INDEX uq_orders_open_per_plan ON maintenance_orders (plan_id) WHERE status IN ('PENDING', 'IN_PROGRESS');
CREATE INDEX idx_orders_scheduled_for ON maintenance_orders (scheduled_for);
CREATE INDEX idx_orders_vehicle_id ON maintenance_orders (vehicle_id);
CREATE INDEX idx_orders_status ON maintenance_orders (status);
