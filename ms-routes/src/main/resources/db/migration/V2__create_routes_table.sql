CREATE TABLE routes (
                        id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        vehicle_id             UUID           NOT NULL,
                        driver_id              UUID           NOT NULL,
                        origin                 VARCHAR(255)   NOT NULL,
                        destination            VARCHAR(255)   NOT NULL,
                        planned_start          TIMESTAMPTZ    NOT NULL,
                        estimated_duration_min INTEGER        NOT NULL,
                        planned_distance_km    NUMERIC(10, 2) NOT NULL,
                        status                 VARCHAR(20)    NOT NULL,
                        started_at             TIMESTAMPTZ,
                        ended_at               TIMESTAMPTZ,
                        start_odometer_km      INTEGER,
                        end_odometer_km        INTEGER,
                        actual_distance_km     NUMERIC(10, 2),
                        notes                  VARCHAR(1000),
                        created_by             UUID           NOT NULL,
                        created_at             TIMESTAMPTZ    NOT NULL,
                        updated_at             TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_routes_vehicle_id ON routes (vehicle_id);
CREATE INDEX idx_routes_driver_id ON routes (driver_id);
CREATE INDEX idx_routes_status ON routes (status);
CREATE INDEX idx_routes_planned_start ON routes (planned_start);