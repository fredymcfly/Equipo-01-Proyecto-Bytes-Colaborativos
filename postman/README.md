# Postman — FleetControl

Colección Postman de la plataforma. Es global, pero **cada microservicio es responsable de su propia carpeta**; ahora mismo solo está implementada la de `ms-fuel`.

## Contenido

| Fichero | Qué es |
|---|---|
| `FleetControl.postman_collection.json` | Colección con las peticiones, agrupadas por servicio. |
| `FleetControl-Local.postman_environment.json` | Entorno para ejecutar todo en local. |

## Importar

1. En Postman: **Import** → selecciona los dos ficheros JSON.
2. Arriba a la derecha, elige el entorno **FleetControl - Local**.

## Variables del entorno

| Variable | Valor por defecto | Uso |
|---|---|---|
| `serviceUrl` | `http://localhost:8087` | URL directa al microservicio (la que se usa por defecto). |
| `gatewayUrl` | `http://localhost:8080` | Alternativa a través de ms-gateway. Cambia `{{serviceUrl}}` por `{{gatewayUrl}}` en una petición para probarla por el gateway. |
| `vehicleId` | UUID de ejemplo | Vehículo usado en las peticiones de `ms-fuel`. Ponlo a un vehículo real de tu base de datos. |
| `token` | vacío | Reservado para el JWT. Todavía no se usa: los endpoints de `ms-fuel` son públicos mientras la seguridad está aplazada. |

## Carpeta `ms-fuel`

Endpoints de `docs/ms-fuel.md`. Cada petición lleva sus propios tests (`pm.test`) de estado y de contrato.

- `POST /api/fuel/refuels` — registra un repostaje.
- `GET /api/fuel/refuels` — listado paginado (y una variante con filtros).
- `GET /api/fuel/consumption?from&to` — consumo por vehículo.
- `GET /api/fuel/stats?from&to&granularity` — serie temporal (DAY, WEEK, MONTH).
- Casos negativos: `400` (litros inválidos, faltan `from`/`to`, granularidad inválida), `409 INVALID_ODOMETER`, `404 VEHICLE_NOT_FOUND` y `503 SERVICE_UNAVAILABLE`.

### Requisitos para que pasen los tests

- `POST /api/fuel/refuels`, `GET /api/fuel/consumption` y el `404`/`409` necesitan **ms-vehicles** en marcha (`VehicleClient`). Si ms-vehicles no responde, el POST devuelve `503` con `service: "ms-vehicles"` — que es justo lo que comprueba la petición de `503`.
- Ajusta `vehicleId` a un vehículo existente. Los UUID de ejemplo del enunciado pueden no existir en tu base de datos.

## Añadir la carpeta de otro servicio

1. Crea una carpeta con el nombre del microservicio (por ejemplo `ms-vehicles`).
2. Añade sus peticiones usando `{{serviceUrl}}` (o `{{gatewayUrl}}`) y las variables compartidas.
3. Añade sus `pm.test` de estado y contrato.
4. Abre un PR contra `dev`; el dueño del servicio es quien lo revisa.
