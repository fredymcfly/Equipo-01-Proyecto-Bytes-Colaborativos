# ms-maintenance (puerto 8086)

## Descripción

Gestiona el mantenimiento programado: planes recurrentes por vehículo y las órdenes de trabajo que se derivan de ellos. Consume ms-vehicles para leer el odómetro y para marcar el vehículo como `IN_MAINTENANCE` mientras dura la intervención.

Consume a: ms-vehicles.

## Reglas de negocio

- Un plan define un intervalo por kilómetros (`intervalKm`), por días (`intervalDays`) o por ambos. Al menos uno es obligatorio y el plan vence cuando se cumple el primero.
- Un vehículo solo puede tener un plan activo por tipo de mantenimiento.
- Una tarea diaria (`@Scheduled`, 06:00 UTC) revisa los planes activos que no tienen una orden abierta y crea una orden `PENDING` si faltan 7 días o menos para `nextDueAt`, o si el odómetro del vehículo está a 500 km o menos de `nextDueKm`.
- Al completar una orden, el plan recalcula su próximo vencimiento a partir de la fecha y del odómetro de ese día.

## Endpoints

### 1. `POST /api/maintenance/plans`

**Acceso:** MANAGER, ADMIN.

**Descripción:** crea un plan de mantenimiento para un vehículo. Si no se indican `lastDoneAt` y `lastDoneKm`, se toman la fecha de hoy y el odómetro actual del vehículo.

**Body:**

```json
{
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "intervalKm": 15000,
  "intervalDays": 365,
  "lastDoneAt": "2026-03-10",
  "lastDoneKm": 32000
}
```

**Response 201 Created:**

```json
{
  "id": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "intervalKm": 15000,
  "intervalDays": 365,
  "lastDoneAt": "2026-03-10",
  "lastDoneKm": 32000,
  "nextDueAt": "2027-03-10",
  "nextDueKm": 47000,
  "active": true
}
```

**Errores:** `404 VEHICLE_NOT_FOUND`; `409 PLAN_ALREADY_EXISTS` si el vehículo ya tiene un plan activo de ese tipo; `400 VALIDATION_ERROR` si no se indica ningún intervalo.

### 2. `GET /api/maintenance/plans`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** lista los planes de mantenimiento.

**Query params:** `vehicle` (UUID, opcional), `type` (enum, opcional) y `active` (boolean, opcional).

**Response 200 OK:** lista de planes con la misma estructura que la respuesta anterior.

### 3. `GET /api/maintenance/orders`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** lista paginada de órdenes de trabajo, ordenada por `scheduledFor` ascendente.

**Query params:**

- `vehicle` (UUID, opcional).
- `status` (enum, opcional), por ejemplo `PENDING`.
- `type` (enum, opcional).
- `dueBefore` (fecha `YYYY-MM-DD`, opcional): órdenes con `scheduledFor` igual o anterior a esa fecha.
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100).

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
      "planId": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a02",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "type": "OIL_CHANGE",
      "status": "PENDING",
      "scheduledFor": "2026-10-08",
      "cost": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 6,
  "totalPages": 1
}
```

### 4. `POST /api/maintenance/orders/{orderId}/start`

**Acceso:** MANAGER, ADMIN.

**Descripción:** inicia una orden pendiente. No lleva body. Cambia la orden a `IN_PROGRESS` y el vehículo a `IN_MAINTENANCE` en ms-vehicles.

**Response 200 OK:** la orden con `status` igual a `IN_PROGRESS` y `startedAt` informado.

**Errores:**

| HTTP | Código | Cuándo |
|---|---|---|
| 404 | `ORDER_NOT_FOUND` | La orden no existe |
| 409 | `INVALID_ORDER_STATE` | La orden no está pendiente |
| 409 | `VEHICLE_NOT_AVAILABLE` | El vehículo está en uso |
| 503 | `SERVICE_UNAVAILABLE` | ms-vehicles no responde |

### 5. `POST /api/maintenance/orders/{orderId}/complete`

**Acceso:** MANAGER, ADMIN.

**Descripción:** completa una orden en curso. Registra el coste, devuelve el vehículo a `AVAILABLE` (actualizando su odómetro si el valor indicado es mayor) y recalcula el plan asociado. `odometerKm` es opcional y, si falta, se usa el odómetro actual del vehículo.

**Body:**

```json
{
  "cost": 185.50,
  "odometerKm": 45210,
  "workshop": "Taller Central Madrid",
  "notes": "Cambio de aceite y filtros"
}
```

**Response 200 OK:**

```json
{
  "id": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
  "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
  "type": "OIL_CHANGE",
  "status": "COMPLETED",
  "completedAt": "2026-10-08T09:40:00Z",
  "cost": 185.50,
  "odometerKm": 45210,
  "workshop": "Taller Central Madrid",
  "plan": {
    "id": "9d4a6e21-0b3c-4a77-8f15-2c6e7b1d3a02",
    "nextDueAt": "2027-10-08",
    "nextDueKm": 60210
  }
}
```

**Errores:** `409 INVALID_ORDER_STATE` si la orden no está en curso; `400 VALIDATION_ERROR` si el coste es negativo; `409 INVALID_ODOMETER` si el odómetro es menor que el del vehículo.

### 6. `GET /api/maintenance/stats`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** serie temporal de órdenes completadas y su coste, con desglose por tipo y recuento de órdenes vencidas o próximas a vencer. Lo consume ms-dashboard.

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
  "totals": { "orders": 9, "cost": 1480.75 },
  "series": [
    { "period": "2026-09-07", "orders": 3, "cost": 420.00 },
    { "period": "2026-09-14", "orders": 4, "cost": 715.25 },
    { "period": "2026-09-21", "orders": 2, "cost": 345.50 }
  ],
  "byType": [
    { "type": "OIL_CHANGE", "orders": 4, "cost": 742.00 },
    { "type": "BRAKE_CHECK", "orders": 3, "cost": 530.75 },
    { "type": "TIRE_ROTATION", "orders": 2, "cost": 208.00 }
  ],
  "upcoming": { "overdue": 1, "dueIn7Days": 5 }
}
```

## Modelo de datos

### MaintenancePlan

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `vehicleId` | UUID | Vehículo (referencia a ms-vehicles) |
| `type` | Enum | Tipo de mantenimiento |
| `intervalKm` | Integer | Intervalo en kilómetros (opcional) |
| `intervalDays` | Integer | Intervalo en días (opcional) |
| `lastDoneAt` | LocalDate | Fecha de la última intervención |
| `lastDoneKm` | Integer | Odómetro en la última intervención |
| `nextDueAt` | LocalDate | Fecha del próximo vencimiento |
| `nextDueKm` | Integer | Odómetro del próximo vencimiento |
| `active` | Boolean | Indica si el plan está vigente |
| `createdAt` | Instant | Fecha de creación |

### MaintenanceOrder

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `planId` | UUID | Plan que la originó |
| `vehicleId` | UUID | Vehículo (referencia a ms-vehicles) |
| `type` | Enum | Tipo de mantenimiento |
| `status` | Enum | Estado de la orden |
| `scheduledFor` | LocalDate | Fecha prevista |
| `startedAt` | Instant | Inicio de la intervención |
| `completedAt` | Instant | Fin de la intervención |
| `odometerKm` | Integer | Odómetro al completar |
| `cost` | BigDecimal | Coste en EUR |
| `workshop` | String | Taller que realizó la intervención |
| `notes` | String | Observaciones |
| `createdAt` | Instant | Fecha de creación |

### Enums

```
MaintenanceType:        OIL_CHANGE, TIRE_ROTATION, BRAKE_CHECK, GENERAL_INSPECTION, LEGAL_INSPECTION
MaintenanceOrderStatus: PENDING, IN_PROGRESS, COMPLETED
```

## Configuración

| Elemento | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/maintenance_db` |
| `VEHICLES_SERVICE_URL` | `http://ms-vehicles:8082` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Credenciales de PostgreSQL (`POSTGRES_USER` y `POSTGRES_PASSWORD`) |
| `JWT_SECRET`, `INTERNAL_API_KEY` | Comunes a los servicios de dominio |
| `SPRING_PROFILES_ACTIVE`, `DEMO_SEED`, `DEMO_DAYS` | Control del seeder |

**Datos demo:** unos 50 planes y 40 órdenes; entre ellas, 5 órdenes pendientes con vencimiento en los próximos 7 días y 1 vencida.

## Historias de usuario relacionadas

### US-12 — Planes de mantenimiento programado (3 puntos, Must, semana 1)

- `POST /api/maintenance/plans` crea el plan y calcula `nextDueAt` y `nextDueKm`.
- Sin ningún intervalo se devuelve `400 VALIDATION_ERROR`, y con un segundo plan activo del mismo tipo para el vehículo, `409 PLAN_ALREADY_EXISTS`.
- Si faltan `lastDoneAt` y `lastDoneKm`, se usan la fecha de hoy y el odómetro actual del vehículo.
- `GET /api/maintenance/plans` filtra por vehículo, tipo y `active`.

### US-13 — Órdenes de mantenimiento: generación y ejecución (5 puntos, Must, semana 2)

- La tarea diaria crea una orden `PENDING` cuando faltan 7 días o menos, o 500 km o menos, para el vencimiento de un plan, y no duplica la orden si ya hay una abierta.
- `GET /api/maintenance/orders` filtra por vehículo, estado, tipo y `dueBefore`.
- `start` deja la orden en `IN_PROGRESS` y el vehículo en `IN_MAINTENANCE`. Con el vehículo en uso devuelve `409 VEHICLE_NOT_AVAILABLE`.
- `complete` registra el coste, devuelve el vehículo a `AVAILABLE` y recalcula `nextDueAt` y `nextDueKm` del plan.
- Una orden en un estado que no corresponde devuelve `409 INVALID_ORDER_STATE`.

### US-17 — Endpoints de estadísticas (con ms-routes, ms-fuel y ms-alerts)

- `GET /api/maintenance/stats` acepta `from`, `to` y `granularity`, con `period` en UTC, periodos vacíos a 0 y totales que coinciden con la suma de la serie.
