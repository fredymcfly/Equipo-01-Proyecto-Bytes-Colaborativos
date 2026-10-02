# ms-auth (puerto 8081)

## Descripción

Gestiona el registro y la autenticación de usuarios. Emite tokens JWT (HS256, validez de 1 hora) con los claims `sub` (id del usuario), `username` y `role`. Los demás servicios validan el token localmente con la variable `JWT_SECRET`; `POST /api/auth/validate` queda disponible para comprobaciones remotas.

No consume a ningún otro servicio.

## Roles

| Rol | Permisos |
|---|---|
| `MANAGER` | Gestiona vehículos, conductores, rutas, repostajes y mantenimientos, consulta el dashboard y reconoce alertas. Es el rol que se asigna al registrarse. |
| `ADMIN` | Todo lo de MANAGER, además de modificar las reglas de alertas y lanzar evaluaciones manuales. Lo crea el seeder del perfil `demo`. |

## Endpoints

### 1. `POST /api/auth/register`

**Acceso:** público.

**Descripción:** registra un nuevo usuario con el rol `MANAGER`.

**Body:**

```json
{
  "username": "carlos_ruiz",
  "email": "carlos@fleetcontrol.com",
  "password": "FleetPass123!"
}
```

**Response 201 Created:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "carlos_ruiz",
  "email": "carlos@fleetcontrol.com",
  "role": "MANAGER",
  "createdAt": "2026-10-05T10:30:00Z"
}
```

**Validaciones:**

- Email único y con formato válido.
- Password de mínimo 8 caracteres, con una mayúscula y un número.
- Username único y sin espacios.

**Errores:**

| HTTP | Código | Cuándo |
|---|---|---|
| 409 | `USER_ALREADY_EXISTS` | Ya existe un usuario con ese email |
| 400 | `VALIDATION_ERROR` | La contraseña tiene menos de 8 caracteres o no tiene mayúscula o número |

```json
{
  "error": "USER_ALREADY_EXISTS",
  "message": "Ya existe un usuario con ese email",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

### 2. `POST /api/auth/login`

**Acceso:** público.

**Descripción:** autentica al usuario y devuelve un token JWT.

**Body:**

```json
{
  "email": "carlos@fleetcontrol.com",
  "password": "FleetPass123!"
}
```

**Response 200 OK:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "MANAGER"
}
```

**Response 401 Unauthorized:**

```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Email o contraseña incorrectos",
  "timestamp": "2026-10-05T10:30:00Z"
}
```

### 3. `POST /api/auth/validate`

**Acceso:** interno (consumido por otros servicios o por herramientas de diagnóstico).

**Descripción:** valida un token JWT y devuelve la información del usuario.

**Headers:**

```
Authorization: Bearer <token>
```

**Response 200 OK:**

```json
{
  "valid": true,
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "carlos_ruiz",
  "role": "MANAGER"
}
```

**Response 401 Unauthorized:**

```json
{
  "valid": false,
  "error": "TOKEN_EXPIRED",
  "message": "El token ha expirado"
}
```

## Modelo de datos

### User

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único |
| `username` | String | Nombre de usuario (único) |
| `email` | String | Correo electrónico (único) |
| `password` | String | Hash bcrypt de la contraseña |
| `role` | Enum | Rol del usuario |
| `createdAt` | Instant | Fecha de registro |
| `updatedAt` | Instant | Fecha de actualización |

### Enums

```
UserRole: MANAGER, ADMIN
```

## Configuración

| Elemento | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/auth_db` |
| `JWT_SECRET` | Secreto HS256, mínimo 32 caracteres |
| `JWT_EXPIRATION` | `3600000` (milisegundos, 1 hora) |
| `DEMO_ADMIN_PASSWORD` | Contraseña del usuario `admin@fleetcontrol.com` (perfil `demo`) |
| `INTERNAL_API_KEY` | Clave compartida para llamadas entre servicios |

**Datos demo:** con el perfil `demo` existe el usuario `admin@fleetcontrol.com` con rol `ADMIN`.

## Historias de usuario relacionadas

### US-02 — Registro e inicio de sesión (3 puntos, Must, semana 1)

- Dado un email y una contraseña válidos, `POST /api/auth/register` crea un usuario MANAGER y la contraseña se guarda con bcrypt.
- Dado un email que ya existe, se devuelve `409 USER_ALREADY_EXISTS`.
- Dada una contraseña de menos de 8 caracteres o sin mayúscula o sin número, se devuelve `400 VALIDATION_ERROR`.
- Dadas unas credenciales correctas, `POST /api/auth/login` devuelve un JWT con validez de 3600 segundos. Con credenciales incorrectas se devuelve `401 INVALID_CREDENTIALS`.
- Con el perfil `demo` existe el usuario ADMIN.

### US-03 — Validación de JWT, roles y clave interna (5 puntos, Must, semana 1)

Afecta a todos los servicios de dominio.

- Sin token en un endpoint protegido se devuelve `401 UNAUTHORIZED`, y con un token caducado `401 TOKEN_EXPIRED`.
- Un MANAGER que llama a un endpoint reservado a ADMIN recibe `403 FORBIDDEN`.
- Una petición con la cabecera `X-Internal-Key` correcta se acepta en los endpoints de llamadas internas, y con una clave incorrecta se devuelve `401`.
- `POST /api/auth/validate` devuelve los datos del usuario cuando el token es válido.
- Todos los errores siguen el contrato común con `error`, `message` y `timestamp`.
