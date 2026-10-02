# ms-vehicles (puerto 8082)

## Descripción

Mantiene el inventario de vehículos de la flota con su estado operativo y su odómetro. Es la fuente de verdad que consultan ms-routes, ms-maintenance, ms-fuel, ms-alerts y ms-dashboard, y también recibe de ellos los cambios de estado.

No consume a ningún otro servicio.

## Endpoints

### 1. `GET /api/vehicles`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** lista paginada de vehículos, filtrable por estado, tipo o placa.

**Query params:**

- `status` (enum, opcional), por ejemplo `AVAILABLE`.
- `type` (enum, opcional), por ejemplo `VAN`.
- `plate` (string, opcional): búsqueda parcial.
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100).

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "make": "Ford",
      "model": "Transit",
      "year": 2022,
      "type": "VAN",
      "fuelType": "DIESEL",
      "tankCapacityL": 80,
      "odometerKm": 45210,
      "status": "AVAILABLE"
    },
    {
      "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a02",
      "plate": "7305-LMN",
      "make": "Renault",
      "model": "Clio",
      "year": 2021,
      "type": "CAR",
      "fuelType": "GASOLINE",
      "tankCapacityL": 45,
      "odometerKm": 61880,
      "status": "IN_USE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 25,
  "totalPages": 2
}
```

### 2. `GET /api/vehicles/summary`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** recuento de vehículos por estado y por tipo. Lo consume ms-dashboard.

**Response 200 OK:**

```json
{
  "total": 25,
  "byStatus": {
    "AVAILABLE": 14,
    "IN_USE": 8,
    "IN_MAINTENANCE": 2,
    "OUT_OF_SERVICE": 1
  },
  "byType": {
    "CAR": 9,
    "VAN": 10,
    "TRUCK": 4,
    "MOTORCYCLE": 2
  }
}
```

### 3. `GET /api/vehicles/{vehicleId}`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** detalle de un vehículo por su ID.

**Response 200 OK:**

```json
{
  "id": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "plate": "4821-KDF",
  "make": "Ford",
  "model": "Transit",
  "year": 2022,
  "type": "VAN",
  "fuelType": "DIESEL",
  "tankCapacityL": 80,
  "odometerKm": 45210,
  "status": "AVAILABLE",
  "createdAt": "2026-07-01T08:00:00Z",
  "updatedAt": "2026-10-05T07:45:00Z"
}
```

**Response 404 Not Found:**

```json
{
  "error": "VEHICLE_NOT_FOUND",
  "message": "No existe un vehículo con el ID proporcionado",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

### 4. `POST /api/vehicles`

**Acceso:** MANAGER, ADMIN.

**Descripción:** da de alta un vehículo. Siempre nace en estado `AVAILABLE`.

**Body:**

```json
{
  "plate": "9012-PQR",
  "make": "Mercedes-Benz",
  "model": "Actros",
  "year": 2023,
  "type": "TRUCK",
  "fuelType": "DIESEL",
  "tankCapacityL": 400,
  "odometerKm": 12000
}
```

**Response 201 Created:** el vehículo creado, con la misma estructura que el detalle.

**Validaciones:**

- Placa obligatoria y única.
- Año entre 1990 y el año siguiente al actual.
- `tankCapacityL` mayor que 0 y `odometerKm` mayor o igual que 0.

**Errores:** `409 VEHICLE_ALREADY_EXISTS` si la placa ya existe; `400 VALIDATION_ERROR` si algún campo no cumple.

### 5. `PUT /api/vehicles/{vehicleId}`

**Acceso:** MANAGER, ADMIN.

**Descripción:** actualiza los datos descriptivos del vehículo: `make`, `model`, `year`, `type`, `fuelType` y `tankCapacityL`. No modifica la placa, el estado ni el odómetro.

**Response 200 OK:** el vehículo actualizado.

**Errores:** `404 VEHICLE_NOT_FOUND`, `400 VALIDATION_ERROR`.

### 6. `PATCH /api/vehicles/{vehicleId}/status`

**Acceso:** MANAGER, ADMIN y llamadas internas (ms-routes y ms-maintenance).

**Descripción:** cambia el estado del vehículo y, de forma opcional, actualiza su odómetro. El odómetro solo puede aumentar.

**Body:**

```json
{
  "status": "AVAILABLE",
  "odometerKm": 45310
}
```

**Response 200 OK:** el vehículo actualizado.

**Transiciones permitidas:**

- `AVAILABLE` → `IN_USE` e `IN_USE` → `AVAILABLE`.
- `AVAILABLE` → `IN_MAINTENANCE` e `IN_MAINTENANCE` → `AVAILABLE`.
- Cualquier estado → `OUT_OF_SERVICE`.
- `OUT_OF_SERVICE` → `AVAILABLE`, solo para ADMIN.

**Errores:**

| HTTP | Código | Cuándo |
|---|---|---|
| 409 | `INVALID_STATUS_TRANSITION` | La transición no está permitida (por ejemplo `IN_USE` → `IN_MAINTENANCE`) |
| 409 | `INVALID_ODOMETER` | El valor es menor que el actual |
| 403 | `FORBIDDEN` | Un MANAGER intenta reactivar un vehículo fuera de servicio |

## Modelo de datos

### Vehicle

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `plate` | String | Placa o matrícula (única) |
| `make` | String | Marca |
| `model` | String | Modelo |
| `year` | Integer | Año de fabricación |
| `type` | Enum | Tipo de vehículo |
| `fuelType` | Enum | Tipo de combustible |
| `tankCapacityL` | Integer | Capacidad del depósito en litros |
| `odometerKm` | Integer | Kilometraje acumulado |
| `status` | Enum | Estado operativo |
| `createdAt` | Instant | Fecha de alta |
| `updatedAt` | Instant | Fecha de actualización |

### Enums

```
VehicleType:   CAR, VAN, TRUCK, MOTORCYCLE
FuelType:      DIESEL, GASOLINE, LPG, HYBRID
VehicleStatus: AVAILABLE, IN_USE, IN_MAINTENANCE, OUT_OF_SERVICE
```

## Configuración

| Elemento | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/vehicles_db` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Credenciales de PostgreSQL (`POSTGRES_USER` y `POSTGRES_PASSWORD`) |
| `JWT_SECRET`, `INTERNAL_API_KEY` | Comunes a los servicios de dominio |
| `SPRING_PROFILES_ACTIVE`, `DEMO_SEED`, `DEMO_DAYS` | Control del seeder |

**Datos demo:** 25 vehículos de los cuatro tipos y de varios estados.

## Historias de usuario relacionadas

### US-06 — Alta, consulta y edición de vehículos (3 puntos, Must, semana 1)

- `POST /api/vehicles` crea el vehículo en estado `AVAILABLE`, y una placa duplicada devuelve `409 VEHICLE_ALREADY_EXISTS`.
- `GET /api/vehicles` es paginado y filtra por `status`, `type` y `plate`.
- `GET /api/vehicles/{vehicleId}` devuelve el detalle, o `404 VEHICLE_NOT_FOUND` si no existe.
- `PUT /api/vehicles/{vehicleId}` actualiza los datos descriptivos sin tocar la placa, el estado ni el odómetro.
- Los datos inválidos, como un año fuera de rango o un depósito menor o igual que 0, devuelven `400 VALIDATION_ERROR` con `details`.

### US-07 — Estado y odómetro de vehículos (3 puntos, Must, semana 1)

- Solo se permiten las transiciones descritas arriba, y cualquier otra devuelve `409 INVALID_STATUS_TRANSITION`.
- El odómetro enviado se guarda solo si es mayor o igual que el actual, y si es menor se devuelve `409 INVALID_ODOMETER`.
- Solo un ADMIN puede pasar un vehículo de `OUT_OF_SERVICE` a `AVAILABLE`, y un MANAGER recibe `403 FORBIDDEN`.
- El endpoint acepta la clave interna, para que lo usen ms-routes y ms-maintenance.

### US-18 — Dashboard: resumen de flota (con ms-dashboard)

- `GET /api/vehicles/summary` devuelve el recuento de vehículos por estado y por tipo.
