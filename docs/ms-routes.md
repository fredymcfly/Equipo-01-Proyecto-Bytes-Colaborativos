# ms-routes (puerto 8085)

## Descripción

Gestiona las rutas de la flota: planificación, ejecución e historial. Consume ms-vehicles y ms-drivers para validar la asignación y para actualizar el estado y el odómetro del vehículo cuando una ruta empieza o termina. Es el origen de la métrica de kilómetros recorridos.

Consume a: ms-vehicles, ms-drivers.

## Reglas de negocio

- El vehículo debe existir y no estar `OUT_OF_SERVICE`.
- El conductor debe existir, estar `ACTIVE` y tener la licencia vigente en la fecha de inicio de la ruta.
- La categoría de la licencia debe ser compatible con el tipo de vehículo: `A` habilita `MOTORCYCLE`, `B` habilita `CAR` y `VAN`, y `C` habilita `CAR`, `VAN` y `TRUCK`.
- No puede haber dos rutas `PLANNED` o `IN_PROGRESS` que se solapen en el tiempo para el mismo vehículo o para el mismo conductor. El intervalo de una ruta va de `plannedStart` a `plannedStart` más `estimatedDurationMin`.

## Endpoints

### 1. `POST /api/routes`

**Acceso:** MANAGER, ADMIN.

**Descripción:** planifica una ruta asignándole un vehículo y un conductor. Valida las reglas de negocio llamando a ms-vehicles y ms-drivers.

**Body:**

```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
  "origin": "Madrid - Centro Logístico",
  "destination": "Valencia - Puerto",
  "plannedStart": "2026-10-06T07:00:00Z",
  "estimatedDurationMin": 240,
  "plannedDistanceKm": 355.0
}
```

**Response 201 Created:** la ruta con `status` igual a `PLANNED` y los campos del modelo que ya tienen valor.

**Errores:**

| HTTP | Código | Cuándo |
|---|---|---|
| 404 | `VEHICLE_NOT_FOUND` / `DRIVER_NOT_FOUND` | El recurso no existe |
| 409 | `VEHICLE_NOT_AVAILABLE` | El vehículo está fuera de servicio |
| 409 | `DRIVER_NOT_ELIGIBLE` | El conductor no está activo, tiene la licencia caducada o su categoría no es compatible |
| 409 | `ROUTE_OVERLAP` | Hay solapamiento |
| 503 | `SERVICE_UNAVAILABLE` | ms-vehicles o ms-drivers no responden |

### 2. `GET /api/routes`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** lista paginada de rutas, ordenada por `plannedStart` descendente.

**Query params:**

- `vehicle` (UUID, opcional) y `driver` (UUID, opcional).
- `status` (enum, opcional), por ejemplo `COMPLETED`.
- `from` y `to` (fecha `YYYY-MM-DD`, opcionales): filtran por `plannedStart`.
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100).

**Response 200 OK:** página con la misma estructura que el listado de ms-vehicles (`content`, `page`, `size`, `totalElements`, `totalPages`), donde cada elemento es una ruta.

### 3. `GET /api/routes/{routeId}`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** detalle de una ruta por su ID.

**Errores:** `404 ROUTE_NOT_FOUND`.

### 4. `POST /api/routes/{routeId}/start`

**Acceso:** MANAGER, ADMIN.

**Descripción:** inicia una ruta planificada. No lleva body. El orden de las operaciones es:

1. Comprobar que la ruta está en `PLANNED`.
2. Consultar el vehículo en ms-vehicles y comprobar que está `AVAILABLE`.
3. Cambiar el vehículo a `IN_USE` en ms-vehicles.
4. Guardar la ruta como `IN_PROGRESS`, con `startedAt` igual a la hora actual y `startOdometerKm` igual al odómetro del vehículo.

Si falla el paso 4, se devuelve el vehículo a `AVAILABLE` como compensación.

**Response 200 OK:** la ruta con `status` igual a `IN_PROGRESS`.

**Errores:**

| HTTP | Código | Cuándo |
|---|---|---|
| 409 | `INVALID_ROUTE_STATE` | La ruta no está planificada |
| 409 | `VEHICLE_NOT_AVAILABLE` | El vehículo está en uso o en mantenimiento |
| 503 | `SERVICE_UNAVAILABLE` | ms-vehicles no responde |

### 5. `POST /api/routes/{routeId}/complete`

**Acceso:** MANAGER, ADMIN.

**Descripción:** finaliza una ruta en curso. Calcula `endOdometerKm` sumando la distancia real, redondeada, a `startOdometerKm`, y deja el vehículo `AVAILABLE` con el odómetro actualizado. Si ms-vehicles no responde, la ruta permanece `IN_PROGRESS` y la operación puede reintentarse.

**Body:**

```json
{
  "actualDistanceKm": 362.4,
  "notes": "Sin incidencias"
}
```

**Response 200 OK:**

```json
{
  "id": "c8e1b7a4-52d9-4f06-8e3b-1a7d90c4f201",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
  "origin": "Madrid - Centro Logístico",
  "destination": "Valencia - Puerto",
  "plannedStart": "2026-10-06T07:00:00Z",
  "estimatedDurationMin": 240,
  "plannedDistanceKm": 355.0,
  "status": "COMPLETED",
  "startedAt": "2026-10-06T07:04:12Z",
  "endedAt": "2026-10-06T11:20:45Z",
  "startOdometerKm": 45210,
  "endOdometerKm": 45572,
  "actualDistanceKm": 362.4,
  "notes": "Sin incidencias"
}
```

**Errores:** `409 INVALID_ROUTE_STATE` si la ruta no está en curso; `400 VALIDATION_ERROR` si `actualDistanceKm` no es mayor que 0.

### 6. `GET /api/routes/stats`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** serie temporal de rutas completadas, agrupadas por la fecha de finalización. Lo consume ms-dashboard.

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
  "totals": { "routes": 302, "distanceKm": 28084.9 },
  "series": [
    { "period": "2026-09-07", "routes": 97, "distanceKm": 9034.1, "avgDurationMin": 128 },
    { "period": "2026-09-14", "routes": 104, "distanceKm": 9710.6, "avgDurationMin": 131 },
    { "period": "2026-09-21", "routes": 101, "distanceKm": 9340.2, "avgDurationMin": 126 }
  ]
}
```

## Modelo de datos

### Route

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `vehicleId` | UUID | Vehículo asignado (referencia a ms-vehicles) |
| `driverId` | UUID | Conductor asignado (referencia a ms-drivers) |
| `origin` | String | Punto de origen |
| `destination` | String | Punto de destino |
| `plannedStart` | Instant | Inicio planificado |
| `estimatedDurationMin` | Integer | Duración estimada en minutos |
| `plannedDistanceKm` | BigDecimal | Distancia planificada |
| `status` | Enum | Estado de la ruta |
| `startedAt` | Instant | Inicio real |
| `endedAt` | Instant | Fin real |
| `startOdometerKm` | Integer | Odómetro al empezar |
| `endOdometerKm` | Integer | Odómetro al terminar |
| `actualDistanceKm` | BigDecimal | Distancia recorrida |
| `notes` | String | Observaciones |
| `createdBy` | UUID | Usuario que planificó la ruta |
| `createdAt` | Instant | Fecha de creación |
| `updatedAt` | Instant | Fecha de actualización |

### Enums

```
RouteStatus: PLANNED, IN_PROGRESS, COMPLETED
```

## Configuración

| Elemento | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/routes_db` |
| `VEHICLES_SERVICE_URL` | `http://ms-vehicles:8082` |
| `DRIVERS_SERVICE_URL` | `http://ms-drivers:8083` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Credenciales de PostgreSQL (`POSTGRES_USER` y `POSTGRES_PASSWORD`) |
| `JWT_SECRET`, `INTERNAL_API_KEY` | Comunes a los servicios de dominio |
| `SPRING_PROFILES_ACTIVE`, `DEMO_SEED`, `DEMO_DAYS` | Control del seeder |

**Datos demo:** unas 1.300 rutas completadas en días laborables y 3 rutas planificadas.

## Historias de usuario relacionadas

### US-09 — Planificación de rutas (5 puntos, Must, semana 1)

- Dado un vehículo y un conductor válidos, `POST /api/routes` crea la ruta en estado `PLANNED`.
- Con un vehículo `OUT_OF_SERVICE` se devuelve `409 VEHICLE_NOT_AVAILABLE`.
- Con un conductor inactivo, con la licencia caducada en la fecha de inicio o con una categoría incompatible, se devuelve `409 DRIVER_NOT_ELIGIBLE`.
- Si el vehículo o el conductor se solapan con otra ruta planificada o en curso, se devuelve `409 ROUTE_OVERLAP`.
- Si ms-vehicles o ms-drivers no responden, se devuelve `503 SERVICE_UNAVAILABLE` y no se guarda nada.
- `GET /api/routes` y `GET /api/routes/{routeId}` funcionan con filtros y paginación.

### US-10 — Inicio y finalización de rutas (5 puntos, Must, semana 2)

- Con una ruta `PLANNED` y un vehículo `AVAILABLE`, `start` deja la ruta en `IN_PROGRESS` y el vehículo en `IN_USE`, y guarda `startedAt` y `startOdometerKm`.
- Con un vehículo en uso o en mantenimiento, `start` devuelve `409 VEHICLE_NOT_AVAILABLE`. Con una ruta que no está planificada devuelve `409 INVALID_ROUTE_STATE`.
- Si falla el guardado de la ruta después de cambiar el vehículo, el vehículo vuelve a `AVAILABLE`.
- `complete` calcula `endOdometerKm`, deja el vehículo en `AVAILABLE` con el odómetro actualizado y la ruta en `COMPLETED`.
- Si ms-vehicles no responde durante `complete`, la ruta sigue `IN_PROGRESS` y la operación se puede reintentar.

### US-17 — Endpoints de estadísticas (con ms-fuel, ms-maintenance y ms-alerts)

- `GET /api/routes/stats` acepta `from`, `to` y `granularity`.
- `period` es la fecha de inicio del periodo, en UTC.
- Los periodos sin datos devuelven 0 y los totales coinciden con la suma de la serie.

### US-24 — Exportación de repostajes y rutas a CSV (2 puntos, Could, fuera del compromiso)

Historia fuera del compromiso del sprint; no forma parte de los endpoints requeridos.

- `GET /api/routes/export` acepta los mismos filtros que el listado y devuelve un CSV en UTF-8 con cabecera.
- Cada exportación devuelve como máximo 10.000 filas.
