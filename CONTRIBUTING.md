# Normas de trabajo del equipo

## 1. Flujo de trabajo con Git

El equipo trabaja sobre la rama `dev`, que es la rama de integración donde se junta el trabajo de todos. La rama `main` recoge únicamente versiones estables del proyecto.

Cada tarea se desarrolla en una rama propia creada a partir de `dev`. Una vez terminada, se abre un Pull Request (PR) hacia `dev`. No se hacen commits directos sobre `dev` ni sobre `main`. *(Se aplicarán reglas al repositorio para ello, pendiente.)*

## 2. Reparto del trabajo

Cada miembro del equipo se hace responsable de un microservicio completo, desde su desarrollo hasta sus tests y su documentación.

## 3. Revisión de Pull Requests

Todo PR necesita la aprobación de al menos una persona del equipo, distinta de su autor, antes de poder fusionarse. El autor no aprueba ni fusiona su propio PR sin esa revisión.

Antes de solicitar la revisión, el autor comprueba que el código compila, que los tests pasan y que no se incluyen credenciales ni secretos.

### Formato del branch

| Branch | Objetivo |
|---|---|
| `main` | Código estable y listo para producción. Solo se incorporan cambios procedentes de la rama de integración `dev`. |
| `dev` | Todas las ramas de funcionalidades se incorporan aquí en primer lugar. |
| `feat/*`, `fix/*`, etc. | Ramas de corta duración para cambios puntuales. |

| Prefijo | Uso |
|---|---|
| `feat/` | Una nueva feature o endpoint |
| `fix/` | Un bug o corregir algo |
| `refactor/` | Reestructuración del código sin cambios en el comportamiento |
| `docs/` | Cambios que afectan únicamente a la documentación |
| `chore/` | Tareas de mantenimiento: dependencias, configuración de CI, scripts de compilación |

Formato: `tipo/numero-issue-descripcion-corta`

Ejemplos:

- `feat/12-login-jwt`
- `fix/27-token-expirado`
- `docs/31-readme-setup`
- `refactor/40-limpiar-reserva-service`
- `chore/50-config-spotless`
- `ci/52-workflow-github-actions`

### Commit conventions

Para mantener un historial de Git limpio, legible y fácilmente auditable, el proyecto se ajusta al estándar [Conventional Commits](https://www.conventionalcommits.org/). Cada mensaje de commit debe seguir estrictamente la siguiente estructura:

```
<tipo>: <descripción corta>
```

| Tipo | Uso |
|---|---|
| `feat` | Nuevas funcionalidades |
| `fix` | Corregir un error |
| `refactor` | Reestructurar el código sin modificar su comportamiento |
| `docs` | Solo cambios en la documentación |
| `chore` | Sistema de compilación, dependencias, CI/CD, configuración |
| `test` | Añadir o actualizar pruebas |

Ejemplos:

- `feat: add rate limiting filter to gateway`
- `fix: validate expired jwt tokens`
- `docs: update readme setup steps`
- `test: add unit tests for reservation service`
- `chore: configure spotless plugin`

### Ejemplo de PR

**Título:** `feat: add rate limiting filter`

**Descripción:**

```markdown
## Work performed
- Added a rate limiting filter to the gateway (100 requests per minute per IP).
- Returns `429 Too Many Requests` when the limit is exceeded.
- Limit values are configurable through `application.yml`.
- Added unit tests for the filter (allowed request, blocked request, counter reset).

## Files created/modified
- Created: `gateway/src/main/java/com/equipo/gateway/filter/RateLimitFilter.java`
- Created: `gateway/src/test/java/com/equipo/gateway/filter/RateLimitFilterTest.java`
- Modified: `gateway/src/main/resources/application.yml` (new `rate-limit` properties)
- Modified: `gateway/pom.xml` (added dependency)

## Notes
- To test it: run `docker compose up` and call any endpoint more than 100 times in a minute.
- The counter is stored in memory, so it resets when the gateway restarts. Redis could be a future improvement.
- Related task: Trello card #12.
```

Definición de cada parte:

- **Work performed:** qué hace el cambio, en viñetas cortas. Piensa en lo que necesita saber quien lo revisa.
- **Files created/modified:** separa lo creado de lo modificado y añade entre paréntesis qué cambió en los ficheros modificados. No hace falta listar cada fichero si son muchos; agrúpalos por carpeta.
- **Notes:** aquí van cómo probarlo, decisiones que has tomado, limitaciones o el enlace a la tarjeta de Trello o la issue. Si no hay nada que añadir, escribe "lo dejamos en blanco".

### Linters

| Herramienta | Uso |
|---|---|
| Spotless | Formateo automático de código |
| Checkstyle | Estilo de código (Google Style) |
| PMD (con reglas propias sencillas) | Análisis estático de código |
| JaCoCo | Cobertura de tests (60 % mínimo) — opcional |
| Surefire | Ejecución de tests unitarios *(¿JaCoCo?, modificar)* |

### Comandos Maven

| Comando | Uso |
|---|---|
| `mvn spotless:apply` | Aplica el formato automáticamente |
| `mvn spotless:check` | Comprueba que el código ya se ajuste al formato |
| `mvn checkstyle:check` | Comprueba que el código cumpla con las normas de estilo predefinidas y las mejores prácticas |
| `mvn pmd:check` | Realiza análisis estático de código para detectar problemas de calidad, errores habituales y malas prácticas |

### Comandos de Docker

| Comando | Uso |
|---|---|
| `docker-compose up --build` | Levanta toda la plataforma |
| `docker-compose up --build -d` | Levanta en segundo plano (detached) |
| `docker-compose down` | Detiene y elimina todos los contenedores |
| `docker-compose down -v` | Elimina también los volúmenes (borra los datos de la base de datos) |
| `docker-compose up nombre-del-servicio` | Levanta y activa un servicio concreto |

## 4. Integración continua (CI/CD)

El repositorio utiliza GitHub Actions. Los workflows acordados son:

| Workflow | Descripción |
|---|---|
| `code-quality` | Ejecuta los linters (Spotless, Checkstyle y PMD) con Java (versión a confirmar). Comprueba que el código cumple las normas de formato y calidad del equipo antes de fusionar un PR. |
| `docker-build` | Construye los contenedores de los microservicios para comprobar que las imágenes se generan correctamente. |

**Ejecución.** Ambos workflows se lanzan en los PR hacia `dev` y `main`.

**Workflow opcional (sin acordar):** `tests`. Ejecuta `mvn test` y genera el informe de JaCoCo. Se plantea como mejora si el equipo lo aprueba, ya que los dos workflows acordados no comprueban que el código funcione, solo su formato y que se construya.

## 5. Comentarios Javadoc

**Formato.** El comentario empieza con una descripción sencilla de una línea que explica qué hace el método, sin repetir su nombre. Las etiquetas `@param`, `@return` y `@throws` se añaden solo cuando aportan información que el nombre no da.

**Autoría.** No se utilizan las etiquetas `@author`, `@version` ni `@since`, ya que el historial de Git recoge quién ha escrito cada cambio.

**Excepciones.** No es necesario documentar getters, setters, constructores triviales ni tests. Los endpoints de los controllers se documentan con Swagger/OpenAPI, no con Javadoc.

**Idioma.** Los comentarios se escriben en el mismo idioma que los commits y el resto del código. *(Pendiente de confirmar.)*

```java
/**
 * Creates a reservation for the given room and dates.
 *
 * @param request room id, guest id and check-in/check-out dates
 * @return the created reservation with its generated id
 * @throws RoomNotAvailableException if the room is already booked for those dates
 */
public ReservationDto createReservation(CreateReservationRequest request) {
    ...
}
```

## 6. Uso de asistentes de IA

El repositorio incluye un fichero `AGENTS.md` en la raíz, donde se recogen las instrucciones para los asistentes de IA (Codex, Claude Code, etc.) que algún miembro del equipo decida utilizar. Su finalidad es que estas herramientas conozcan las normas del proyecto y las respeten: estructura, comandos, flujo de Git y convenciones de código.

El uso de IA es opcional y cada persona elige su herramienta. El código generado con IA se trata igual que cualquier otro: quien lo sube debe entenderlo y revisarlo, y pasa por los mismos linters, tests y revisión de PR.

## 7. Estructura del proyecto (ejemplo visual)

```
proyecto/
├── .github/
│   ├── workflows/
│   │   ├── code-quality
│   │   └── docker-build
│   └── pull_request_template.md
├── config/
│   ├── PMD
│   └── Spotless
├── docs/
│   ├── documentación
│   └── arquitectura
├── gateway/
├── security/
├── [servicio 3]/
├── [servicio 4]/
├── .env.example
├── .gitignore
├── docker-compose.yml
├── AGENTS.md
└── README.md
```

Estructura interna de cada microservicio (ejemplo con `gateway`):

```
gateway/
├── src/
│   ├── main/
│   │   ├── java/com/equipo/gateway/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   ├── dto/
│   │   │   ├── mapper/
│   │   │   ├── config/
│   │   │   └── exception/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/equipo/gateway/
├── Dockerfile
├── .dockerignore
└── pom.xml
```

## 8. Pasos para iniciar el proyecto

1. Clonar el repositorio.
2. Copiar el `.env.example`:
   ```bash
   cp .env.example .env
   ```
3. Hacer el build del docker-compose. Como estamos en la fase inicial, para que no tarde el proceso, se puede levantar únicamente la base de datos de Docker:
   ```bash
   docker-compose up postgres-datasource
   ```
   Si quieres levantar todos los microservicios:
   ```bash
   docker-compose up --build
   ```
4. Crear una nueva rama para trabajar (ver el apartado "Formato del branch").

### Pasos para subir el código

1. Pasar los linters del proyecto:
   ```bash
   mvn spotless:apply
   mvn checkstyle:check
   mvn pmd:check
   ```
2. Una vez pasados los linters, subir la rama al repositorio:
   ```bash
   git push -u origin nombre-de-tu-rama
   ```

### Pasos para hacer el Pull Request

1. El Pull Request tiene que ser a la rama `dev`.
2. Asignar tu perfil al PR y rellenar los campos (labels, projects, milestone) y una breve descripción:
   - Work performed
   - Files created/modified
   - Notes (opcional)
3. Verificar que pasa el workflow de GitHub Actions.
4. Terminar de mergear el código a `dev`.
