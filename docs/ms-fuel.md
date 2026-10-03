# ms-fuel (puerto 8087)

## Descripción

Registra los repostajes de la flota y calcula el consumo de cada vehículo. Consume ms-vehicles para validar el vehículo, conocer su tipo de combustible y la capacidad de su depósito, y completar las respuestas con la placa. Es el origen de las métricas de litros, coste y consumo.

Consume a: ms-vehicles.

## Reglas de negocio

- `fuelType` se toma del vehículo; no se envía en el body.
- `liters` no puede superar la capacidad del depósito del vehículo.
- `odometerKm` no puede ser menor que el del último repostaje del vehículo.
- `totalCost` es `liters` por `pricePerLiter`, redondeado a 2 decimales.
- El consumo se calcula entre dos repostajes con depósito lleno consecutivos (`fullTank` igual a `true`), sumando los litros de los repostajes parciales intermedios:

```
consumo (L/100 km) = (litros desde el lleno anterior / km entre llenos) × 100
```

Por ejemplo, con un lleno a 45.000 km, un repostaje parcial de 20 L y un nuevo lleno de 38 L a 45.500 km, el consumo es (20 + 38) / 500 × 100 = 11,6 L/100 km. Si no existe un lleno anterior, `consumptionL100km` es `null`.

## Endpoints

### 1. `POST /api/fuel/refuels`

**Acceso:** MANAGER, ADMIN.

**Descripción:** registra un repostaje.

**Body:**

```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "refueledAt": "2026-10-06T12:15:00Z",
  "liters": 58.4,
  "pricePerLiter": 1.62,
  "odometerKm": 45572,
  "fullTank": true,
  "station": "Repsol A-3 km 112"
}
```

**Response 201 Created:**

```json
{
  "id": "b7a3f9e2-4c18-4d5a-9e60-3f1d8c2a7b01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "refueledAt": "2026-10-06T12:15:00Z",
  "fuelType": "DIESEL",
  "liters": 58.4,
  "pricePerLiter": 1.62,
  "totalCost": 94.61,
  "odometerKm": 45572,
  "fullTank": true,
  "station": "Repsol A-3 km 112",
  "consumptionL100km": 11.3
}
```

**Errores:**

| HTTP | Código | Cuándo |
|---|---|---|
| 404 | `VEHICLE_NOT_FOUND` | El vehículo no existe |
| 400 | `VALIDATION_ERROR` | `liters` es 0 o negativo, o supera la capacidad del depósito |
| 409 | `INVALID_ODOMETER` | El odómetro es menor que el del repostaje anterior |
| 503 | `SERVICE_UNAVAILABLE` | ms-vehicles no responde |

### 2. `GET /api/fuel/refuels`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** lista paginada de repostajes, ordenada por `refueledAt` descendente.

**Query params:**

- `vehicle` (UUID, opcional).
- `from` y `to` (fecha `YYYY-MM-DD`, opcionales).
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100).

**Response 200 OK:** página con la estructura habitual (`content`, `page`, `size`, `totalElements`, `totalPages`), donde cada elemento tiene los mismos campos que la respuesta de creación.

### 3. `GET /api/fuel/consumption`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** consumo agregado por vehículo en un periodo, ordenado por `cost` descendente. La distancia se calcula con la diferencia de odómetro entre el primer y el último repostaje del periodo. La placa y el tipo se obtienen de ms-vehicles. Lo consumen ms-dashboard y ms-alerts.

**Query params:** `from` y `to` (fecha `YYYY-MM-DD`, requeridos).

**Response 200 OK:**

```json
{
  "from": "2026-09-01",
  "to": "2026-09-30",
  "vehicles": [
    {
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03",
      "plate": "9012-PQR",
      "type": "TRUCK",
      "liters": 1845.2,
      "cost": 2951.96,
      "distanceKm": 6720,
      "avgL100km": 27.5
    },
    {
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "type": "VAN",
      "liters": 690.3,
      "cost": 1118.29,
      "distanceKm": 6310,
      "avgL100km": 10.9
    }
  ]
}
```

### 4. `GET /api/fuel/stats`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** serie temporal de litros, coste y consumo medio de la flota o de un vehículo. Lo consume ms-dashboard.

**Query params:**

- `from` y `to` (fecha `YYYY-MM-DD`, requeridos).
- `granularity` (enum, opcional, por defecto `DAY`): `DAY`, `WEEK` o `MONTH`.
- `vehicle` (UUID, opcional).

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "liters": 3090.0, "cost": 5005.80, "avgPricePerLiter": 1.62, "avgL100km": 11.0 },
  "series": [
    { "period": "2026-09-07", "liters": 984.7, "cost": 1595.21, "avgPricePerLiter": 1.62, "avgL100km": 10.9 },
    { "period": "2026-09-14", "liters": 1077.9, "cost": 1746.20, "avgPricePerLiter": 1.62, "avgL100km": 11.1 },
    { "period": "2026-09-21", "liters": 1027.4, "cost": 1664.39, "avgPricePerLiter": 1.62, "avgL100km": 11.0 }
  ]
}
```

## Modelo de datos

### FuelRecord

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `vehicleId` | UUID | Vehículo (referencia a ms-vehicles) |
| `refueledAt` | Instant | Fecha y hora del repostaje |
| `fuelType` | Enum | Tipo de combustible, tomado del vehículo |
| `liters` | BigDecimal | Litros repostados |
| `pricePerLiter` | BigDecimal | Precio por litro en EUR |
| `totalCost` | BigDecimal | Coste total en EUR |
| `odometerKm` | Integer | Odómetro en el momento del repostaje |
| `fullTank` | Boolean | Indica si se llenó el depósito |
| `station` | String | Estación de servicio |
| `consumptionL100km` | BigDecimal | Consumo calculado (nulo si no hay lleno anterior) |
| `createdAt` | Instant | Fecha de registro |

### Enums

```
FuelType: DIESEL, GASOLINE, LPG, HYBRID   // mismos valores que en ms-vehicles
```

## Configuración

| Elemento | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/fuel_db` |
| `VEHICLES_SERVICE_URL` | `http://ms-vehicles:8082` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Credenciales de PostgreSQL (`POSTGRES_USER` y `POSTGRES_PASSWORD`) |
| `JWT_SECRET`, `INTERNAL_API_KEY` | Comunes a los servicios de dominio |
| `SPRING_PROFILES_ACTIVE`, `DEMO_SEED`, `DEMO_DAYS` | Control del seeder |

**Datos demo:** unos 300 repostajes con consumo coherente con el tipo de vehículo (±8 % sobre el consumo base y precio entre 1,45 y 1,75 EUR por litro). Dos vehículos tienen un consumo un 30 % superior a su media anterior en los últimos 30 días.

## Historias de usuario relacionadas

### US-11 — Repostajes y consumo (5 puntos, Must, semana 1)

- `POST /api/fuel/refuels` toma el combustible del vehículo, calcula `totalCost` con 2 decimales y devuelve `consumptionL100km` entre depósitos llenos, o `null` si no hay un lleno anterior.
- Con litros por encima de la capacidad del depósito se devuelve `400 VALIDATION_ERROR`, y con un odómetro menor que el del repostaje anterior, `409 INVALID_ODOMETER`.
- `GET /api/fuel/refuels` es paginado y filtra por vehículo y fechas.
- `GET /api/fuel/consumption` devuelve por vehículo los litros, el coste, la distancia y el consumo medio, con la placa obtenida de ms-vehicles.
- El cálculo del consumo tiene tests unitarios que incluyen el ejemplo (20 + 38) / 500 × 100 = 11,6 L/100 km.

### US-17 — Endpoints de estadísticas (con ms-routes, ms-maintenance y ms-alerts)

- `GET /api/fuel/stats` acepta `from`, `to` y `granularity`, con `period` en UTC, periodos vacíos a 0 y totales que coinciden con la suma de la serie.

### US-24 — Exportación de repostajes y rutas a CSV (2 puntos, Could, fuera del compromiso)

Historia fuera del compromiso del sprint; no forma parte de los endpoints requeridos.

- `GET /api/fuel/refuels/export` acepta los mismos filtros que el listado y devuelve un CSV en UTF-8 con cabecera.
- Cada exportación devuelve como máximo 10.000 filas.
