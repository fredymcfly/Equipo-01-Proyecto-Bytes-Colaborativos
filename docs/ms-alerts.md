# ms-alerts (puerto 8088)

## Descripción

Detecta situaciones que requieren atención y las convierte en alertas: mantenimientos próximos o vencidos, licencias de conducir próximas a caducar o caducadas y consumo de combustible anormal. Un evaluador programado consulta ms-maintenance, ms-drivers y ms-fuel, y usa ms-vehicles para completar los mensajes con la placa.

Consume a: ms-maintenance, ms-drivers, ms-fuel, ms-vehicles.

## Reglas de evaluación

| Tipo de alerta | Condición | Severidad por defecto | Parámetros |
|---|---|---|---|
| `MAINTENANCE_DUE` | Orden `PENDING` con `scheduledFor` entre hoy y los próximos `daysBefore` días | `WARNING` | `daysBefore`: 7 |
| `MAINTENANCE_OVERDUE` | Orden `PENDING` con `scheduledFor` anterior a hoy | `CRITICAL` | — |
| `LICENSE_EXPIRING` | Licencia que caduca en `daysBefore` días o menos | `WARNING` | `daysBefore`: 30 |
| `LICENSE_EXPIRED` | Licencia con fecha de caducidad anterior a hoy | `CRITICAL` | — |
| `HIGH_CONSUMPTION` | Consumo medio de los últimos 30 días superior en `thresholdPercent` % al de los 60 días anteriores | `WARNING` | `thresholdPercent`: 25 |

### Funcionamiento del evaluador

- Se ejecuta cada 15 minutos (`alerts.evaluation.interval-ms`, por defecto 900000), con un retraso inicial de 60 segundos para esperar a que arranquen los demás servicios.
- Cada alerta tiene una `dedupKey` única, por ejemplo `MAINTENANCE_DUE:<orderId>`, de modo que una misma condición no genera alertas duplicadas.
- Si la condición deja de cumplirse, la alerta pasa a `RESOLVED` en la siguiente evaluación.
- Si una dependencia no responde, el evaluador omite esa regla en esa ronda y continúa con las demás.

## Endpoints

### 1. `GET /api/alerts`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** lista paginada de alertas, ordenada por severidad descendente y fecha de creación descendente.

**Query params:**

- `status` (enum, opcional), por ejemplo `OPEN`.
- `severity` (enum, opcional) y `type` (enum, opcional).
- `vehicle` (UUID, opcional).
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100).

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a01",
      "type": "MAINTENANCE_OVERDUE",
      "severity": "CRITICAL",
      "status": "OPEN",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a05",
      "driverId": null,
      "message": "Mantenimiento OIL_CHANGE vencido desde el 2026-09-28 en el vehículo 3342-HJK",
      "createdAt": "2026-10-05T06:15:00Z",
      "acknowledgedAt": null
    },
    {
      "id": "d1a5c7e9-3f42-4b8a-a6d0-9e2b4c8f1a02",
      "type": "LICENSE_EXPIRING",
      "severity": "WARNING",
      "status": "ACKNOWLEDGED",
      "vehicleId": null,
      "driverId": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
      "message": "La licencia de Miguel Torres caduca el 2026-10-20",
      "createdAt": "2026-10-04T06:15:00Z",
      "acknowledgedAt": "2026-10-04T09:12:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 12,
  "totalPages": 1
}
```

### 2. `POST /api/alerts/{alertId}/acknowledge`

**Acceso:** MANAGER, ADMIN.

**Descripción:** marca una alerta abierta como reconocida. El body es opcional.

**Body:**

```json
{
  "comment": "Cita pedida con el taller para el jueves"
}
```

**Response 200 OK:** la alerta con `status` igual a `ACKNOWLEDGED`, y `acknowledgedAt` y `acknowledgedBy` informados.

**Errores:** `404 ALERT_NOT_FOUND`; `409 INVALID_ALERT_STATE` si la alerta no está en `OPEN`.

### 3. `GET /api/alerts/rules`

**Acceso:** MANAGER, ADMIN.

**Descripción:** lista la configuración de las reglas de evaluación.

**Response 200 OK:**

```json
[
  { "type": "MAINTENANCE_DUE", "enabled": true, "severity": "WARNING", "params": { "daysBefore": 7 } },
  { "type": "MAINTENANCE_OVERDUE", "enabled": true, "severity": "CRITICAL", "params": {} },
  { "type": "LICENSE_EXPIRING", "enabled": true, "severity": "WARNING", "params": { "daysBefore": 30 } },
  { "type": "LICENSE_EXPIRED", "enabled": true, "severity": "CRITICAL", "params": {} },
  { "type": "HIGH_CONSUMPTION", "enabled": true, "severity": "WARNING", "params": { "thresholdPercent": 25 } }
]
```

### 4. `PUT /api/alerts/rules/{type}`

**Acceso:** ADMIN.

**Descripción:** modifica una regla: activarla o desactivarla, cambiar su severidad o ajustar sus parámetros. El cambio se aplica en la siguiente evaluación.

**Body:**

```json
{
  "enabled": true,
  "severity": "CRITICAL",
  "params": { "daysBefore": 14 }
}
```

**Response 200 OK:** la regla actualizada.

**Errores:** `404 RULE_NOT_FOUND`; `400 VALIDATION_ERROR` si un parámetro no es válido para ese tipo de regla; `403 FORBIDDEN` si el usuario es MANAGER.

### 5. `POST /api/alerts/evaluate`

**Acceso:** ADMIN.

**Descripción:** lanza una evaluación inmediata, sin esperar al evaluador programado. Resulta útil para probar las reglas.

**Response 200 OK:**

```json
{
  "created": 3,
  "resolved": 1,
  "skipped": ["HIGH_CONSUMPTION"],
  "durationMs": 842
}
```

`skipped` lista los tipos de regla que no se evaluaron porque una dependencia no respondió.

### 6. `GET /api/alerts/stats`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** serie temporal de alertas creadas, con desglose por severidad, y recuento de alertas abiertas. Lo consume ms-dashboard.

**Query params:**

- `from` y `to` (fecha `YYYY-MM-DD`, requeridos).
- `granularity` (enum, opcional, por defecto `DAY`): `DAY`, `WEEK` o `MONTH`.

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "totals": { "created": 11 },
  "series": [
    { "period": "2026-09-07", "created": 3, "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 } },
    { "period": "2026-09-14", "created": 5, "bySeverity": { "INFO": 0, "WARNING": 3, "CRITICAL": 2 } },
    { "period": "2026-09-21", "created": 3, "bySeverity": { "INFO": 0, "WARNING": 2, "CRITICAL": 1 } }
  ],
  "open": { "INFO": 0, "WARNING": 9, "CRITICAL": 2 }
}
```

## Modelo de datos

### Alert

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `type` | Enum | Tipo de alerta |
| `severity` | Enum | Severidad |
| `status` | Enum | Estado de la alerta |
| `vehicleId` | UUID | Vehículo afectado (opcional) |
| `driverId` | UUID | Conductor afectado (opcional) |
| `message` | String | Mensaje legible |
| `dedupKey` | String | Clave única de la condición que la originó |
| `createdAt` | Instant | Fecha de creación |
| `acknowledgedAt` | Instant | Fecha de reconocimiento |
| `acknowledgedBy` | UUID | Usuario que la reconoció |
| `comment` | String | Comentario del reconocimiento |
| `resolvedAt` | Instant | Fecha de resolución |

### AlertRule

| Campo | Tipo | Descripción |
|---|---|---|
| `type` | Enum | Tipo de alerta a la que aplica (clave primaria) |
| `enabled` | Boolean | Indica si la regla está activa |
| `severity` | Enum | Severidad que se asigna a las alertas |
| `params` | JSON | Parámetros de la regla |
| `updatedAt` | Instant | Fecha de la última modificación |

### Enums

```
AlertType:     MAINTENANCE_DUE, MAINTENANCE_OVERDUE, LICENSE_EXPIRING, LICENSE_EXPIRED, HIGH_CONSUMPTION
AlertSeverity: INFO, WARNING, CRITICAL
AlertStatus:   OPEN, ACKNOWLEDGED, RESOLVED
```

## Configuración

| Elemento | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/alerts_db` |
| `VEHICLES_SERVICE_URL` | `http://ms-vehicles:8082` |
| `DRIVERS_SERVICE_URL` | `http://ms-drivers:8083` |
| `MAINTENANCE_SERVICE_URL` | `http://ms-maintenance:8086` |
| `FUEL_SERVICE_URL` | `http://ms-fuel:8087` |
| `ALERTS_EVALUATION_INTERVAL_MS` | `900000` (15 minutos) |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Credenciales de PostgreSQL (`POSTGRES_USER` y `POSTGRES_PASSWORD`) |
| `JWT_SECRET`, `INTERNAL_API_KEY` | Comunes a los servicios de dominio |

**Datos demo:** este servicio no genera datos. El evaluador crea las alertas a partir de los demás servicios.

## Historias de usuario relacionadas

### US-14 — Evaluador de alertas (8 puntos, Must, semana 2)

- El evaluador se ejecuta cada 15 minutos y genera alertas para las 5 reglas.
- Una misma condición no genera alertas duplicadas gracias a `dedupKey`.
- Cuando la condición desaparece, la alerta pasa a `RESOLVED` en la siguiente evaluación.
- Si una dependencia no responde, se omite esa regla, se informa en `skipped` y las demás reglas se evalúan igualmente.
- `GET /api/alerts` es paginado, filtra por estado, severidad, tipo y vehículo, y ordena por severidad.
- `POST /api/alerts/evaluate`, solo para ADMIN, lanza la evaluación y devuelve `created`, `resolved`, `skipped` y `durationMs`.

### US-15 — Gestión y configuración de alertas (2 puntos, Should, semana 2)

- `POST /api/alerts/{alertId}/acknowledge` pasa la alerta de `OPEN` a `ACKNOWLEDGED` y guarda usuario, fecha y comentario. Con otro estado devuelve `409 INVALID_ALERT_STATE`.
- `GET /api/alerts/rules` lista las 5 reglas con su configuración.
- `PUT /api/alerts/rules/{type}` solo lo puede usar un ADMIN, valida los parámetros y se aplica en la siguiente evaluación.

### US-17 — Endpoints de estadísticas (con ms-routes, ms-fuel y ms-maintenance)

- `GET /api/alerts/stats` acepta `from`, `to` y `granularity`, con `period` en UTC, periodos vacíos a 0 y totales que coinciden con la suma de la serie.

### US-23 — Notificación de alertas críticas por webhook (3 puntos, Could, fuera del compromiso)

Historia fuera del compromiso del sprint; no forma parte de los endpoints requeridos.

- Con `WEBHOOK_URL` configurada, cada alerta `CRITICAL` nueva se envía en un POST con el JSON de la alerta.
- Si el envío falla, se reintenta 3 veces y se registra el error, sin bloquear el evaluador.
