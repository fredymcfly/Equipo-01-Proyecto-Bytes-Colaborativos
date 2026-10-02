# ms-dashboard (puerto 8089)

## Descripción

Agrega datos de los demás microservicios en un resumen ejecutivo de la flota y en series temporales listas para representarse en gráficos. No tiene base de datos: guarda las respuestas en una caché Caffeine con una validez de 60 segundos.

Consume a: ms-vehicles, ms-routes, ms-fuel, ms-maintenance, ms-alerts.

## Reglas de agregación

- Las llamadas a los servicios de origen se lanzan en paralelo.
- Si un servicio de origen no responde, el dashboard devuelve los datos disponibles con `partial` igual a `true` y la lista `unavailable` con los servicios afectados. Los valores que dependen de ese servicio se devuelven como `null`.
- La caché usa como clave el endpoint y todos sus parámetros.

## Endpoints

### 1. `GET /api/dashboard`

**Acceso:** MANAGER, ADMIN.

**Descripción:** resumen principal de la flota para los últimos días: estado de los vehículos, indicadores clave, alertas abiertas y próximos mantenimientos.

**Query params:** `days` (int, opcional, por defecto 30, entre 1 y 365).

**Response 200 OK:**

```json
{
  "generatedAt": "2026-10-05T10:30:00Z",
  "period": { "from": "2026-09-05", "to": "2026-10-05" },
  "partial": false,
  "unavailable": [],
  "fleet": {
    "total": 25,
    "available": 14,
    "inUse": 8,
    "inMaintenance": 2,
    "outOfService": 1
  },
  "kpis": {
    "routesCompleted": 412,
    "distanceKm": 38420.5,
    "fuelLiters": 4226.3,
    "fuelCost": 6846.61,
    "avgConsumptionL100km": 11.0,
    "maintenanceCost": 2140.25
  },
  "alerts": { "open": 11, "critical": 2, "warning": 9, "info": 0 },
  "upcomingMaintenance": [
    {
      "orderId": "e5b8c3f7-91a2-4d60-b7c4-0a1f6d2e8b01",
      "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a01",
      "plate": "4821-KDF",
      "type": "OIL_CHANGE",
      "scheduledFor": "2026-10-08"
    }
  ]
}
```

**Origen de cada bloque:**

| Bloque | Origen |
|---|---|
| `fleet` | ms-vehicles |
| `kpis` | Estadísticas de ms-routes, ms-fuel y ms-maintenance |
| `alerts` | ms-alerts |
| `upcomingMaintenance` | Las 5 próximas órdenes pendientes de ms-maintenance |

### 2. `GET /api/dashboard/timeseries`

**Acceso:** MANAGER, ADMIN.

**Descripción:** series temporales de una o varias métricas con la misma granularidad, listas para un gráfico de líneas o de barras.

**Query params:**

- `metrics` (lista separada por comas, requerido). Valores: `DISTANCE_KM`, `FUEL_LITERS`, `FUEL_COST`, `CONSUMPTION_L100KM`, `MAINTENANCE_COST`, `ALERTS_CREATED`.
- `from` y `to` (fecha `YYYY-MM-DD`, requeridos). El rango máximo es de 366 días.
- `granularity` (enum, opcional, por defecto `DAY`): `DAY`, `WEEK` o `MONTH`.

**Response 200 OK:**

```json
{
  "from": "2026-09-07",
  "to": "2026-09-27",
  "granularity": "WEEK",
  "partial": false,
  "unavailable": [],
  "series": [
    {
      "metric": "DISTANCE_KM",
      "unit": "km",
      "points": [
        { "period": "2026-09-07", "value": 9034.1 },
        { "period": "2026-09-14", "value": 9710.6 },
        { "period": "2026-09-21", "value": 9340.2 }
      ]
    },
    {
      "metric": "FUEL_COST",
      "unit": "EUR",
      "points": [
        { "period": "2026-09-07", "value": 1595.21 },
        { "period": "2026-09-14", "value": 1746.20 },
        { "period": "2026-09-21", "value": 1664.39 }
      ]
    }
  ]
}
```

**Errores:** `400 VALIDATION_ERROR` si alguna métrica no existe, si falta un parámetro requerido o si el rango supera los 366 días.

### 3. `GET /api/dashboard/vehicles/ranking`

**Acceso:** MANAGER, ADMIN.

**Descripción:** clasificación de vehículos por coste de combustible o por consumo, a partir de `GET /api/fuel/consumption`. El resultado va ordenado de mayor a menor valor.

**Query params:**

- `metric` (enum, requerido): `FUEL_COST` o `CONSUMPTION`.
- `days` (int, opcional, por defecto 30).
- `limit` (int, opcional, por defecto 5, máximo 20).
- `type` (enum, opcional): limita el ranking a un tipo de vehículo, útil para comparar consumos de vehículos equivalentes.

**Response 200 OK** (ejemplo abreviado):

```json
{
  "metric": "CONSUMPTION",
  "unit": "L/100 km",
  "period": { "from": "2026-09-05", "to": "2026-10-05" },
  "ranking": [
    { "rank": 1, "vehicleId": "6f1c0a52-3b7e-4d1f-9a21-8c4d5e6f7a03", "plate": "9012-PQR", "type": "TRUCK", "value": 27.5 }
  ]
}
```

Cada elemento de `ranking` lleva `rank`, `vehicleId`, `plate`, `type` y `value`.

## Modelo de datos

Este servicio no tiene persistencia. Solo mantiene la caché en memoria descrita arriba.

## Configuración

| Elemento | Valor |
|---|---|
| Base de datos | Ninguna |
| `VEHICLES_SERVICE_URL` | `http://ms-vehicles:8082` |
| `ROUTES_SERVICE_URL` | `http://ms-routes:8085` |
| `FUEL_SERVICE_URL` | `http://ms-fuel:8087` |
| `MAINTENANCE_SERVICE_URL` | `http://ms-maintenance:8086` |
| `ALERTS_SERVICE_URL` | `http://ms-alerts:8088` |
| `JWT_SECRET`, `INTERNAL_API_KEY` | Comunes a los servicios de dominio |

## Historias de usuario relacionadas

### US-18 — Dashboard: resumen de flota (5 puntos, Must, semana 2)

Servicios: ms-dashboard y ms-vehicles.

- `GET /api/vehicles/summary` devuelve el recuento de vehículos por estado y por tipo.
- `GET /api/dashboard` devuelve `fleet`, `kpis`, `alerts` y `upcomingMaintenance` para los últimos `days` días.
- Las llamadas a los servicios de origen se lanzan en paralelo y la respuesta se guarda en caché durante 60 segundos.
- Si un servicio de origen no responde, el endpoint devuelve 200 con `partial` igual a `true`, la lista `unavailable` y los valores afectados a `null`.
- Con los datos demo, la respuesta tarda menos de 2 segundos sin caché.

### US-19 — Dashboard: series temporales y ranking (3 puntos, Should, semana 2)

- `GET /api/dashboard/timeseries` acepta `metrics`, `from`, `to` y `granularity`, y devuelve una serie por cada métrica pedida.
- Una métrica desconocida o un rango superior a 366 días devuelve `400 VALIDATION_ERROR`.
- `GET /api/dashboard/vehicles/ranking` ordena de mayor a menor por `FUEL_COST` o `CONSUMPTION`, y acepta `days`, `limit` y `type`.
- Si un servicio de origen no responde, solo se omiten sus métricas y se informa en `unavailable`.
