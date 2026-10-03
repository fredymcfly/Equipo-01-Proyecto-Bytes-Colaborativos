# ms-gateway (puerto 8080)

## Descripción

Punto de entrada único al sistema. Redirige cada petición al microservicio correcto y aplica rate limiting global. Además genera la cabecera `X-Request-Id` cuando la petición no la trae y elimina `X-Internal-Key` de todas las peticiones externas, para que ningún cliente pueda hacerse pasar por un servicio interno.

Consume a todos los servicios. No tiene base de datos.

## Tabla de enrutamiento

| Prefijo de ruta | Destino |
|---|---|
| `/api/auth/**` | ms-auth:8081 |
| `/api/vehicles/**` | ms-vehicles:8082 |
| `/api/drivers/**` | ms-drivers:8083 |
| `/api/routes/**` | ms-routes:8085 |
| `/api/maintenance/**` | ms-maintenance:8086 |
| `/api/fuel/**` | ms-fuel:8087 |
| `/api/alerts/**` | ms-alerts:8088 |
| `/api/dashboard/**` | ms-dashboard:8089 |

## Endpoints

### 1. `ANY /api/**`

**Acceso:** público (reenvía al servicio correspondiente, que valida el JWT).

**Descripción:** proxy inverso. Redirige la petición y devuelve la respuesta del servicio destino sin modificarla. Si el destino no responde en 5 segundos, devuelve `503 SERVICE_UNAVAILABLE`.

### 2. `GET /health`

**Acceso:** público.

**Descripción:** estado del gateway y de los servicios downstream. Consulta el `/actuator/health` de cada servicio con un timeout de 2 segundos y marca como `DOWN` los que no responden. Siempre devuelve 200.

**Response 200 OK:**

```json
{
  "gateway": "UP",
  "timestamp": "2026-10-05T10:30:00Z",
  "services": {
    "ms-auth": "UP",
    "ms-vehicles": "UP",
    "ms-drivers": "UP",
    "ms-routes": "UP",
    "ms-maintenance": "UP",
    "ms-fuel": "UP",
    "ms-alerts": "UP",
    "ms-dashboard": "DOWN"
  }
}
```

## Rate limiting

**Límite:** 60 requests por minuto por IP. Se configura con una variable de entorno.

Pasado el minuto, el contador se reinicia y la IP puede volver a llamar.

**Response 429 Too Many Requests** (con la cabecera `Retry-After`):

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Demasiadas peticiones. Límite: 60 req/min",
  "retryAfter": 30,
  "timestamp": "2026-10-05T10:30:00Z"
}
```

## Configuración

| Variable | Valor en Docker Compose |
|---|---|
| `AUTH_SERVICE_URL` | `http://ms-auth:8081` |
| `VEHICLES_SERVICE_URL` | `http://ms-vehicles:8082` |
| `DRIVERS_SERVICE_URL` | `http://ms-drivers:8083` |
| `ROUTES_SERVICE_URL` | `http://ms-routes:8085` |
| `MAINTENANCE_SERVICE_URL` | `http://ms-maintenance:8086` |
| `FUEL_SERVICE_URL` | `http://ms-fuel:8087` |
| `ALERTS_SERVICE_URL` | `http://ms-alerts:8088` |
| `DASHBOARD_SERVICE_URL` | `http://ms-dashboard:8089` |

## Historias de usuario relacionadas

### US-04 — Gateway: enrutamiento y health (3 puntos, Must, semana 1)

- Dada una petición con cualquiera de los prefijos de la tabla de enrutamiento, se reenvía al servicio correcto y se devuelve su respuesta sin modificar.
- Si el servicio destino no responde en 5 segundos, se devuelve `503 SERVICE_UNAVAILABLE`.
- `GET /health` devuelve el estado de los 8 servicios y marca como `DOWN` los que no responden.
- El gateway añade `X-Request-Id` cuando falta y elimina `X-Internal-Key` de las peticiones externas.

### US-05 — Gateway: rate limiting (2 puntos, Should, semana 1)

- Dadas más de 60 peticiones en un minuto desde la misma IP, se devuelve `429 RATE_LIMIT_EXCEEDED` con `retryAfter` y la cabecera `Retry-After`.
- Pasado el minuto, el contador se reinicia y la IP puede volver a llamar.
- El límite se configura con una variable de entorno.
