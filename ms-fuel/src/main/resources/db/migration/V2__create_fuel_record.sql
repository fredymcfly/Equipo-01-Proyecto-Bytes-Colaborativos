-- Repostajes de la flota. El tipo de combustible y el vehículo viven en ms-vehicles,
-- aqui solo se guarda el identificador del vehiculo.
CREATE TABLE fuel_records (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    refueled_at TIMESTAMPTZ NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    liters NUMERIC(10, 2) NOT NULL,
    price_per_liter NUMERIC(8, 2) NOT NULL,
    total_cost NUMERIC(12, 2) NOT NULL,
    odometer_km INTEGER NOT NULL,
    full_tank BOOLEAN NOT NULL,
    station VARCHAR(150),
    consumption_l100km NUMERIC(6, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Soportan el listado por vehiculo y las series temporales de /stats.
CREATE INDEX idx_fuel_records_vehicle_refueled_at
    ON fuel_records (vehicle_id, refueled_at DESC);

CREATE INDEX idx_fuel_records_refueled_at
    ON fuel_records (refueled_at DESC);