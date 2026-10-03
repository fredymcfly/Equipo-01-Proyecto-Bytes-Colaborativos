# Contrato común

Reglas que cumplen todos los microservicios de FleetControl.

## Convenciones

- Los identificadores son UUID.
- Los instantes usan ISO-8601 en UTC (`2026-10-05T10:30:00Z`) y las fechas sin hora usan `YYYY-MM-DD`.
- La paginación usa `page` desde 0 y `size` con valor por defecto 20 y máximo 100. La respuesta incluye `content`, `page`, `size`, `totalElements` y `totalPages`.
- Los importes van en EUR con 2 decimales, las distancias en km, los volúmenes en litros y el consumo en L/100 km.
- Cada servicio expone `/actuator/health` y su Swagger UI en `/swagger-ui.html`.

## Contrato de errores

Todos los servicios devuelven los errores con esta estructura:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "La petición contiene campos no válidos",
  "details": [
    { "field": "year", "reason": "debe estar entre 1990 y 2027" }
  ],
  "timestamp": "2026-10-05T10:30:00Z"
}
```

`details` solo aparece en `VALIDATION_ERROR`. En `SERVICE_UNAVAILABLE` se añade el campo `service` con el nombre del servicio que no responde.

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Algún campo no cumple las validaciones |
| 401 | `UNAUTHORIZED`, `TOKEN_EXPIRED` | Falta el token, no es válido o ha caducado |
| 403 | `FORBIDDEN` | El rol del usuario no tiene permiso |
| 404 | `VEHICLE_NOT_FOUND`, `DRIVER_NOT_FOUND`, `ROUTE_NOT_FOUND`, `ORDER_NOT_FOUND`, `ALERT_NOT_FOUND`, `RULE_NOT_FOUND` | El recurso no existe |
| 409 | Códigos de negocio de cada servicio | La operación entra en conflicto con el estado actual |
| 429 | `RATE_LIMIT_EXCEEDED` | Se supera el límite del gateway |
| 503 | `SERVICE_UNAVAILABLE` | Una dependencia no responde |

## Autenticación y roles

- **Peticiones externas:** cabecera `Authorization: Bearer <token>`. Cada servicio verifica la firma HS256 con `JWT_SECRET`, lee los claims `sub`, `username` y `role`, y usa el rol para decidir el acceso.
- **Endpoints públicos:** `POST /api/auth/register`, `POST /api/auth/login`, `GET /health` del gateway, `/actuator/health` y Swagger UI.
- **Llamadas entre servicios:** cabecera `X-Internal-Key` con el valor de `INTERNAL_API_KEY`. Se usa también en las tareas programadas, que no tienen un token de usuario. El servicio que la recibe correcta trata la llamada con los permisos de un MANAGER. Los endpoints marcados como "llamadas internas" aceptan el JWT o esta clave.
- **Protección:** el gateway elimina `X-Internal-Key` de las peticiones externas, para que ningún cliente pueda hacerse pasar por un servicio interno.

| Rol | Permisos |
|---|---|
| `MANAGER` | Gestiona vehículos, conductores, rutas, repostajes y mantenimientos, consulta el dashboard y reconoce alertas. Es el rol que se asigna al registrarse. |
| `ADMIN` | Todo lo de MANAGER, además de modificar las reglas de alertas y lanzar evaluaciones manuales. Lo crea el seeder del perfil `demo`. |

## Comunicación entre servicios

La comunicación es síncrona por HTTP con OpenFeign.

### Mapa de dependencias

| Servicio que llama | Servicios a los que llama |
|---|---|
| ms-gateway | Todos (enrutamiento) |
| ms-dashboard | ms-vehicles, ms-routes, ms-fuel, ms-maintenance, ms-alerts |
| ms-alerts | ms-maintenance, ms-fuel, ms-drivers, ms-vehicles |
| ms-routes | ms-vehicles, ms-drivers |
| ms-maintenance | ms-vehicles |
| ms-fuel | ms-vehicles |
| ms-auth, ms-vehicles, ms-drivers | — |

### Llamadas que desencadena cada acción

| Acción | Servicio que la inicia | Llamadas que desencadena |
|---|---|---|
| Planificar una ruta | ms-routes | ms-vehicles: consultar el vehículo. ms-drivers: consultar el conductor |
| Iniciar una ruta | ms-routes | ms-vehicles: consultar el vehículo y cambiarlo a `IN_USE` |
| Completar una ruta | ms-routes | ms-vehicles: cambiarlo a `AVAILABLE` y actualizar el odómetro |
| Registrar un repostaje | ms-fuel | ms-vehicles: consultar el tipo de combustible y la capacidad del depósito |
| Crear un plan de mantenimiento | ms-maintenance | ms-vehicles: comprobar que existe y leer el odómetro |
| Iniciar una orden de mantenimiento | ms-maintenance | ms-vehicles: cambiarlo a `IN_MAINTENANCE` |
| Completar una orden de mantenimiento | ms-maintenance | ms-vehicles: cambiarlo a `AVAILABLE` y actualizar el odómetro |
| Tarea diaria de mantenimiento | ms-maintenance | ms-vehicles: leer el odómetro de cada vehículo con plan |
| Evaluación de alertas | ms-alerts | ms-maintenance: órdenes pendientes. ms-drivers: licencias. ms-fuel: consumo. ms-vehicles: placas |
| Consulta del dashboard | ms-dashboard | ms-vehicles, ms-routes, ms-fuel, ms-maintenance y ms-alerts: resúmenes y estadísticas |

### Clientes Feign

Ejemplo en ms-routes consumiendo ms-vehicles:

```java
@FeignClient(name = "ms-vehicles", url = "${vehicles.service.url}",
             configuration = InternalFeignConfig.class)
public interface VehicleClient {

    @GetMapping("/api/vehicles/{vehicleId}")
    VehicleResponse getVehicle(@PathVariable UUID vehicleId);

    @PatchMapping("/api/vehicles/{vehicleId}/status")
    VehicleResponse updateStatus(@PathVariable UUID vehicleId,
                                 @RequestBody VehicleStatusRequest request);
}
```

Configuración común que añade la clave interna a cada llamada:

```java
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalKeyInterceptor(
            @Value("${internal.api-key}") String apiKey) {
        return template -> template.header("X-Internal-Key", apiKey);
    }
}
```

## Resiliencia

- **Timeouts:** 2 segundos de conexión y 5 de lectura en todos los clientes Feign.
- **Reintentos:** un reintento, solo en peticiones GET.
- **Circuit breaker (Resilience4j):** uno por cliente. Se abre cuando falla el 50 % de las últimas 10 llamadas y espera 30 segundos antes de volver a probar.
- **Dependencia caída:** el servicio responde `503 SERVICE_UNAVAILABLE`, salvo ms-dashboard y ms-alerts, que continúan con los datos disponibles como se describe en cada servicio.

## Series temporales

Los servicios que poseen datos con fecha exponen `GET /stats` con los parámetros `from`, `to` y `granularity` (`DAY`, `WEEK` o `MONTH`). ms-dashboard combina esas series. Las reglas son comunes a todos:

- `period` es la fecha de inicio del periodo: el propio día en `DAY`, el lunes en `WEEK` y el día 1 en `MONTH`.
- Los periodos sin datos se devuelven con valor 0, para que la serie no tenga huecos.
- Todas las fechas se calculan en UTC.

| Métrica | Servicio de origen | Unidad |
|---|---|---|
| `DISTANCE_KM` | ms-routes | km |
| `FUEL_LITERS` | ms-fuel | L |
| `FUEL_COST` | ms-fuel | EUR |
| `CONSUMPTION_L100KM` | ms-fuel | L/100 km |
| `MAINTENANCE_COST` | ms-maintenance | EUR |
| `ALERTS_CREATED` | ms-alerts | alertas |

## Datos de demostración

Cada servicio de dominio genera su propio histórico de 90 días al arrancar con el perfil `demo`, de modo que el dashboard muestra series temporales desde la primera ejecución.

### Reglas del seeder

- Se activa con `SPRING_PROFILES_ACTIVE=demo` y se controla con `DEMO_SEED` (por defecto 42) y `DEMO_DAYS` (por defecto 90).
- Es idempotente: solo inserta datos si las tablas del servicio están vacías.
- Las fechas son relativas al arranque (los últimos `DEMO_DAYS` días), por lo que los datos siempre parecen recientes.
- Los identificadores son UUID deterministas derivados de la semilla, por ejemplo `UUID.nameUUIDFromBytes(...)` sobre el texto `vehicle-7`. Así ms-routes, ms-fuel y ms-maintenance referencian los mismos vehículos y conductores sin llamarse durante el arranque.
- ms-auth crea el usuario `admin@fleetcontrol.com` con rol ADMIN. Su contraseña se lee de la variable `DEMO_ADMIN_PASSWORD`.
- Para regenerar el histórico desde cero: `docker compose down -v` y volver a levantar el sistema.

### Volumen generado

| Servicio | Datos generados | Volumen aproximado |
|---|---|---|
| ms-vehicles | Vehículos de los cuatro tipos y de varios estados | 25 |
| ms-drivers | Conductores; 3 con licencia próxima a caducar y 1 caducada | 20 |
| ms-routes | Rutas completadas en días laborables y 3 rutas planificadas | ≈ 1.300 |
| ms-fuel | Repostajes con consumo coherente con el tipo de vehículo | ≈ 300 |
| ms-maintenance | Planes por vehículo y órdenes completadas; 5 órdenes pendientes con vencimiento en los próximos 7 días y 1 vencida | ≈ 50 planes y ≈ 40 órdenes |
| ms-alerts | No genera datos. El evaluador crea las alertas a partir de los demás servicios | — |

### Realismo de los datos

| Tipo de vehículo | Consumo base (L/100 km) |
|---|---|
| `CAR` | 6,5 |
| `VAN` | 10,5 |
| `TRUCK` | 27 |
| `MOTORCYCLE` | 4,2 |

Cada repostaje varía un ±8 % sobre el consumo base y el precio por litro oscila entre 1,45 y 1,75 EUR con tendencia semanal. Dos vehículos tienen en los últimos 30 días un consumo un 30 % superior a su media anterior, para que ms-alerts genere alertas de consumo alto desde el primer arranque.

## Variables de entorno

Se definen en el fichero `.env` de la raíz (ver `.env.example`).

| Variable | Descripción |
|---|---|
| `POSTGRES_USER` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Secreto HS256, mínimo 32 caracteres |
| `INTERNAL_API_KEY` | Clave compartida para las llamadas entre servicios |
| `DEMO_ADMIN_PASSWORD` | Contraseña del usuario `admin@fleetcontrol.com` |
| `SPRING_PROFILES_ACTIVE` | `demo` activa el seeder |
| `DEMO_SEED` | Semilla del generador de datos (42 por defecto) |
| `DEMO_DAYS` | Días de histórico a generar (90 por defecto) |

Docker Compose las pasa a cada servicio junto con estas variables de Spring, que no se escriben en el `.env`:

| Variable | Descripción |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de la base de datos del servicio (`jdbc:postgresql://postgres:5432/<servicio>_db`) |
| `SPRING_DATASOURCE_USERNAME` | Toma el valor de `POSTGRES_USER` (`fleet` por defecto) |
| `SPRING_DATASOURCE_PASSWORD` | Toma el valor de `POSTGRES_PASSWORD` |
