# ms-drivers (puerto 8083)

## Descripción

Gestiona los conductores de la flota, su licencia de conducir y su estado. ms-routes lo consulta para comprobar que un conductor puede asignarse a una ruta, y ms-alerts para detectar licencias próximas a caducar.

No consume a ningún otro servicio.

## Endpoints

### 1. `GET /api/drivers`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** lista paginada de conductores, filtrable por estado o por caducidad de la licencia.

**Query params:**

- `status` (enum, opcional), por ejemplo `ACTIVE`.
- `licenseExpiringInDays` (int, opcional): devuelve los conductores cuya licencia caduca en ese número de días o menos, incluidas las ya caducadas.
- `page` (int, opcional, por defecto 0) y `size` (int, opcional, por defecto 20, máximo 100).

**Response 200 OK:**

```json
{
  "content": [
    {
      "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a01",
      "fullName": "Laura Fernández",
      "email": "laura.fernandez@fleetcontrol.com",
      "phone": "+34 600 123 456",
      "licenseNumber": "B-4471923",
      "licenseCategory": "B",
      "licenseExpiresAt": "2027-03-14",
      "status": "ACTIVE"
    },
    {
      "id": "3b2d8f10-7c4a-4e55-b1a0-5d9e2c7f1a02",
      "fullName": "Miguel Torres",
      "email": "miguel.torres@fleetcontrol.com",
      "phone": "+34 611 987 654",
      "licenseNumber": "C-2038841",
      "licenseCategory": "C",
      "licenseExpiresAt": "2026-10-20",
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 20,
  "totalPages": 1
}
```

### 2. `GET /api/drivers/{driverId}`

**Acceso:** MANAGER, ADMIN y llamadas internas.

**Descripción:** detalle de un conductor por su ID. Devuelve los mismos campos que el listado más `createdAt` y `updatedAt`.

**Response 404 Not Found:**

```json
{
  "error": "DRIVER_NOT_FOUND",
  "message": "No existe un conductor con el ID proporcionado",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

### 3. `POST /api/drivers`

**Acceso:** MANAGER, ADMIN.

**Descripción:** da de alta un conductor. Siempre nace en estado `ACTIVE`.

**Body:**

```json
{
  "fullName": "Sara Ibáñez",
  "email": "sara.ibanez@fleetcontrol.com",
  "phone": "+34 622 456 789",
  "licenseNumber": "B-5120377",
  "licenseCategory": "B",
  "licenseExpiresAt": "2029-06-30"
}
```

**Response 201 Created:** el conductor creado, con `id`, `status`, `createdAt` y `updatedAt`.

**Validaciones:**

- Número de licencia obligatorio y único.
- Email con formato válido.
- `licenseExpiresAt` posterior a la fecha actual.

**Errores:** `409 DRIVER_ALREADY_EXISTS` si el número de licencia ya existe; `400 VALIDATION_ERROR` si algún campo no cumple.

### 4. `PUT /api/drivers/{driverId}`

**Acceso:** MANAGER, ADMIN.

**Descripción:** actualiza todos los datos del conductor, incluido su estado (`ACTIVE`, `ON_LEAVE`, `SUSPENDED`). Permite renovar la licencia con una nueva fecha de caducidad.

**Response 200 OK:** el conductor actualizado.

**Errores:** `404 DRIVER_NOT_FOUND`, `409 DRIVER_ALREADY_EXISTS`, `400 VALIDATION_ERROR`.

## Modelo de datos

### Driver

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `fullName` | String | Nombre completo |
| `email` | String | Correo electrónico |
| `phone` | String | Teléfono de contacto |
| `licenseNumber` | String | Número de licencia (único) |
| `licenseCategory` | Enum | Categoría de la licencia |
| `licenseExpiresAt` | LocalDate | Fecha de caducidad de la licencia |
| `status` | Enum | Estado del conductor |
| `createdAt` | Instant | Fecha de alta |
| `updatedAt` | Instant | Fecha de actualización |

### Enums

```
LicenseCategory: A, B, C
DriverStatus:    ACTIVE, ON_LEAVE, SUSPENDED
```

## Configuración

| Elemento | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/drivers_db` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Credenciales de PostgreSQL (`POSTGRES_USER` y `POSTGRES_PASSWORD`) |
| `JWT_SECRET`, `INTERNAL_API_KEY` | Comunes a los servicios de dominio |
| `SPRING_PROFILES_ACTIVE`, `DEMO_SEED`, `DEMO_DAYS` | Control del seeder |

**Datos demo:** 20 conductores; 3 con licencia próxima a caducar y 1 caducada.

## Historias de usuario relacionadas

### US-08 — Gestión de conductores (3 puntos, Must, semana 1)

- `POST /api/drivers` crea el conductor en estado `ACTIVE`. Una licencia duplicada devuelve `409 DRIVER_ALREADY_EXISTS` y una fecha de caducidad pasada devuelve `400 VALIDATION_ERROR`.
- `GET /api/drivers` es paginado y filtra por `status` y por `licenseExpiringInDays`, incluyendo las licencias ya caducadas.
- `PUT /api/drivers/{driverId}` actualiza los datos y el estado, y permite renovar la licencia con una nueva fecha de caducidad.
- `GET /api/drivers/{driverId}` devuelve el detalle, o `404 DRIVER_NOT_FOUND` si no existe.
