# BOHEME — Entregable 2, apartado 2.7: seguridad JWT

## Resultado y comprobaciones

Implementación de registro, login, BCrypt, JWT, roles, propiedad de recursos y
persistencia de cuentas en H2. Se conservan los diez endpoints previos y se
añaden dos de autenticación. No se hicieron commits ni push; no se avanzó a 2.8/2.9.

Comprobado el 24 de octubre de 2026:

- `.\gradlew.bat clean test`: **BUILD SUCCESSFUL**, **189 pruebas**,
  **0 fallos, 0 errores y 0 omitidas**. Se conservan las 140 anteriores y se añaden 49.
- **43 peticiones HTTP reales** con los códigos esperados.
- **3 consultas SQL** desde la consola H2 de la misma instancia Spring.
- **12 operaciones OpenAPI**, esquema HTTP Bearer y seguridad por operación.
- Registros y login reales de compradores y vendedores, rechazo de UUID ajenos,
  firma alterada, roles incorrectos, ausencia de token y consultas públicas anónimas.

**Pendiente de comprobación visual:** la herramienta de control del navegador
detuvo la interacción con Chrome porque no pudo determinar su URL con suficiente
confianza. No se llegó a pulsar Authorize ni Execute en Swagger. La configuración
Bearer, las restricciones por operación y el funcionamiento HTTP de los JWT sí
están verificados con pruebas automatizadas y peticiones reales. No se presenta
la interacción visual como una comprobación realizada.

Evidencias permanentes:

- [Pruebas](evidencias-2.7/pruebas.json), extraídas de los XML de JUnit.
- [HTTP, claims sin token y SQL](evidencias-2.7/verificacion.json).
- [Usuarios y formato BCrypt en H2](evidencias-2.7/usuarios-h2.html).
- [Propiedad de prendas](evidencias-2.7/propiedad-prendas-h2.html).
- [Propiedad de bolsas](evidencias-2.7/propiedad-bolsas-h2.html).

Los JWT completos se omiten de los archivos de evidencia. No se guarda el secreto
de firma. Los correos, nombres y contraseñas de demostración son ficticios.

## Arquitectura implementada

`REST → casos de uso → puertos → adaptadores JPA/BCrypt/Nimbus`.

El dominio contiene `Usuario`, `Rol` y `UsuarioRepository`, sin imports de Spring
Security, Spring ni JPA. `Usuario` tiene constructor privado e invariantes:
UUID, nombre, correo normalizado, hash BCrypt, rol COMPRADOR/VENDEDOR y estado activo.
No contiene contraseña plana ni implementa `UserDetails`.

Se inspeccionó `Creador` antes de crear la cuenta. Es un perfil de negocio con
teléfono, taller y vitrinas obligatorias; no sirve para representar también a
compradores ni tiene credenciales. Se conserva sin cambios. No existe una entidad
de cliente persistida que se pueda reutilizar para la autenticación.

`UsuarioJpaEntity` es independiente de `Usuario`. La tabla `usuarios` utiliza UUID
como PK y una restricción única `uk_usuario_correo`. El correo se normaliza con
`trim` y minúsculas antes de persistir/consultar. El adaptador usa `saveAndFlush`:
además de la comprobación previa del caso de uso, la restricción de H2 evita
duplicados concurrentes y se traduce a 409.

Los puertos de aplicación `CodificadorContrasenas`, `EmisorTokens` y `UsuarioActual`
mantienen las dependencias de seguridad fuera del dominio. Los controllers
validan DTO, invocan casos de uso y mappers; no comparan propietarios ni acceden a JPA.

### Casos de uso nuevos

| Caso de uso | Responsabilidad |
|---|---|
| `RegistrarUsuarioUseCase` | Normalizar correo, comprobar duplicado, validar contraseña y persistir cuenta con hash |
| `IniciarSesionUseCase` | Verificar contraseña y estado, emitir token o error genérico |
| `AutorizarRecursosUseCase` | Comprobar identidad y propietario persistido de prendas/bolsas |

Los casos de uso originales conservan sus firmas y comportamiento de negocio.
Las operaciones REST protegidas invocan el caso de autorización antes del caso
de negocio existente. No se añaden reglas de seguridad a las entidades antiguas.

## Relación entre la identidad y los recursos

Una cuenta tiene un único rol y una identidad UUID generada por el servidor:

| Rol | Asociación |
|---|---|
| VENDEDOR | `usuario.id = PrendaDeAutor.creadorId` |
| COMPRADOR | `usuario.id = BolsaDeEstilo.clienteId` |

El cliente **no puede elegir el UUID de su cuenta** durante el registro. Los DTO
de creación de prenda/bolsa conservan `creadorId`/`clienteId` para compatibilidad:
deben coincidir con `sub` del JWT validado. Si son ajenos se devuelve 403 y no se
crea el recurso. Las consultas de bolsa activa validan también el cliente de la URL.

Para suspender/actualizar prendas o vaciar/cerrar bolsas, el caso de autorización
carga el agregado mediante el contrato de repositorio y compara su propietario
con la identidad autenticada. Recurso existente ajeno → 403; inexistente o
eliminado lógicamente → 404. El caso de negocio sólo se ejecuta después del permiso.
Los propietarios no son modificables mediante las operaciones REST existentes.

Las cuentas demo se crean con los UUID de `DatosIniciales`, conservando las
asociaciones de sus tres prendas y dos bolsas. No se cambia el significado de
los UUID ni se crean perfiles ficticios adicionales de `Creador`.

## Dependencias efectivas y seguridad moderna

Gradle comprobó estas versiones, sin fijar versiones independientes de Security:

| Componente | Versión / configuración |
|---|---|
| Java | 21.0.12.1 |
| Spring Boot | 4.1.1 |
| Spring Security | **7.1.1**, gestionada por Boot |
| Starter de ejecución | `spring-boot-starter-security-oauth2-resource-server` |
| Starter de pruebas | `spring-boot-starter-security-test` |
| JWT | Nimbus mediante `NimbusJwtEncoder` y `NimbusJwtDecoder` de Spring Security |

Se usa `SecurityFilterChain`, DSL lambda, `requestMatchers` y Resource Server.
No se usa `WebSecurityConfigurerAdapter`, filtros JWT caseros ni sesiones de login.
Los builders Nimbus utilizados están disponibles en Spring Security 7.
Referencias oficiales consultadas:
[Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html),
[builder HMAC de Nimbus](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/oauth2/jwt/NimbusJwtEncoder.SecretKeyJwtEncoderBuilder.html),
[modularización de Boot y pruebas](https://spring.io/blog/2025/10/28/modularizing-spring-boot/).

## Registro, login, BCrypt y JWT

### POST /api/auth/register — público

Datos: `nombre`, `correo`, `password`, `rol`.

- Nombre obligatorio, máximo 100 caracteres.
- Correo válido, máximo 254 caracteres; se normaliza para unicidad.
- Contraseña obligatoria, mínimo 12 caracteres, máximo **72 bytes UTF-8**.
  Se comprueban bytes para evitar truncamiento o errores de BCrypt con Unicode.
- Rol obligatorio, exclusivamente COMPRADOR o VENDEDOR; ADMIN, ordinales numéricos
  y nombres de rol desconocidos devuelven 400.
- UUID y estado activo se deciden en el servidor.
- Respuesta **201** con `UsuarioResponse`; duplicado **409**; entrada inválida **400**.

BCrypt usa coste **12** y sal generada por el encoder para cada contraseña.
La tabla almacena sólo `password_hash`, de 60 caracteres. Las respuestas no
contienen contraseñas ni hashes. El dominio rechaza un hash en texto plano.

### POST /api/auth/login — público

Datos: `correo`, `password`. Respuesta **200**:

| Campo | Tipo / significado |
|---|---|
| `token` | JWT firmado |
| `tokenType` | `Bearer` |
| `expiresIn` | Segundos configurados de validez |
| `expiresAt` | Fecha de vencimiento UTC |
| `rol` | COMPRADOR o VENDEDOR |
| `usuario` | UUID, nombre, correo, rol y activo |

Correo inexistente, contraseña incorrecta y cuenta inactiva producen el mismo
401 genérico. Se realiza una verificación BCrypt con hash de relleno para un
correo inexistente, evitando omitir el coste del cálculo en ese caso.

El token contiene `sub` (UUID), `correo`, `rol`, `iss=boheme`, `iat`, `exp` y `jti`.
No incluye contraseña, hash ni datos de bolsas. Firma HMAC **HS256** con secreto
Base64 de al menos 32 bytes. El decoder acepta exclusivamente HS256, valida
firma, emisor, expiración sin tolerancia adicional, fecha de emisión, UUID y claims obligatorios.

Después de validar el JWT, se comprueba en H2 que el usuario existe, sigue activo
y conserva el correo y rol del token. Un token firmado de una cuenta inactiva,
inexistente o con claims que ya no coinciden se rechaza con 401.

### Configuración

En `application.properties`:

```properties
boheme.jwt.secret=${BOHEME_JWT_SECRET:}
boheme.jwt.duration=${BOHEME_JWT_DURATION:PT1H}
spring.h2.console.enabled=false
boheme.datos-iniciales.enabled=false
```

Sin secreto, con Base64 inválido o con menos de 32 bytes, el arranque falla con
un mensaje de configuración sin imprimir el valor. La duración debe estar entre
un segundo y 24 horas. No hay secreto de producción publicado ni valor de firma
predeterminado en Java. Las pruebas tienen una clave ficticia separada en
`src/test/resources/application.properties`.

La API usa `SessionCreationPolicy.STATELESS`. Se deshabilitan form login, Basic,
logout por sesión y request cache. CSRF se deshabilita para la API porque la
autenticación viaja en la cabecera Bearer y no en cookies de sesión. Las peticiones
reales comprobaron la ausencia de `Set-Cookie`.

## Tabla de rutas

| Método | Ruta | Acceso | Comprobación de identidad/propiedad |
|---|---|---|---|
| POST | `/api/auth/register` | Público | UUID generado por servidor |
| POST | `/api/auth/login` | Público | Correo, BCrypt, activo |
| GET | `/api/prendas` | Público | No aplica |
| GET | `/api/prendas/creador/{creadorId}` | Público | No aplica |
| GET | `/api/reportes/creadores/variedad` | Público | No aplica |
| POST | `/api/prendas` | VENDEDOR | creadorId = cuenta |
| PATCH | `/api/prendas/{prendaId}/suspender` | VENDEDOR | Propietario persistido |
| PATCH | `/api/prendas/{prendaId}/variantes/{varianteId}/stock` | VENDEDOR | Propietario persistido; variante del agregado |
| POST | `/api/bolsas` | COMPRADOR | clienteId = cuenta |
| GET | `/api/bolsas/activas/{clienteId}` | COMPRADOR | clienteId = cuenta |
| DELETE | `/api/bolsas/{bolsaId}/items` | COMPRADOR | Propietario persistido |
| PATCH | `/api/bolsas/{bolsaId}/cerrar` | COMPRADOR | Propietario persistido |
| GET | `/swagger-ui.html`, `/swagger-ui/**`, `/api-docs`, `/api-docs/**` | Público | No aplica |
| Varios | `/h2-console/**` | Sólo perfil local dev | Consola desactivada y ruta denegada fuera de dev |

Siete operaciones de negocio están restringidas por rol. Sin JWT válido devuelven
401; JWT de una cuenta con rol distinto devuelve 403, antes de validar el cuerpo
de la operación. Un rol correcto no concede acceso a recursos ajenos.

## Errores y consistencia JSON

El entry point y el access denied handler de Spring Security delegan al
`HandlerExceptionResolver`, que utiliza `ApiExceptionHandler`. Se conserva
`ErrorResponse(timestamp,status,code,message,path,details)`.

| HTTP | Código / causa |
|---|---|
| 400 | `DATOS_INVALIDOS`: validación, UUID, tipos, JSON o rol inválido |
| 401 | `NO_AUTENTICADO`: credenciales, token ausente/inválido/vencido, cuenta inactiva |
| 403 | `ACCESO_DENEGADO`: rol insuficiente o propietario ajeno |
| 404 | `RECURSO_NO_ENCONTRADO`: recurso inexistente |
| 409 | `CORREO_DUPLICADO` o códigos previos de reglas de negocio |
| 500 | `ERROR_INTERNO`, mensaje genérico sin trazas ni secretos |

Las respuestas 401 incluyen `WWW-Authenticate: Bearer`. Firma incorrecta y
credenciales incorrectas no exponen detalles internos de validación.

## Swagger Authorize

`OpenApiConfig` declara `bearerAuth` con `type=http`, `scheme=bearer` y
`bearerFormat=JWT`. El customizer añade `security: [{bearerAuth: []}]` y respuestas
401/403 exclusivamente a las siete operaciones protegidas. Las cinco operaciones
públicas de negocio/autenticación no requieren credenciales en OpenAPI.

Se comprobó por HTTP y pruebas que el esquema existe, que las doce operaciones
se conservan y que la protección por operación coincide con la tabla anterior.
El recurso `/swagger-ui/index.html` responde 200.

Comprobación visual que queda pendiente por la limitación de la herramienta:

1. Abrir Swagger en el puerto real y ejecutar `/api/auth/login` con una cuenta demo.
2. Copiar sólo `token`, pulsar **Authorize**, pegarlo y autorizar `bearerAuth`.
3. Ejecutar una operación del rol autenticado usando el UUID de esa cuenta.
   Swagger debe enviar `Authorization: Bearer <token>` y obtener la respuesta documentada.

No se afirmó haber completado esos clics: el control de Chrome se detuvo antes
de interactuar con la página. Esta incidencia es de verificación visual, no un
fallo observado de la firma ni de la cadena de seguridad.

## Desarrollo local y credenciales ficticias

El perfil `dev` activa los datos de negocio, los usuarios demo y la consola H2.
Enlaza el servidor a **127.0.0.1**, impide conexiones remotas a la consola y limita
`frameOptions.sameOrigin()` y la excepción de acceso a una cadena exclusiva
`/h2-console/**`. No relaja las cabeceras del resto de la API.

| Correo demo | Rol | UUID de negocio |
|---|---|---|
| `vendedor1@boheme.test` | VENDEDOR | `81000000-0000-4000-8000-000000000001` |
| `vendedor2@boheme.test` | VENDEDOR | `81000000-0000-4000-8000-000000000002` |
| `comprador1@boheme.test` | COMPRADOR | `c1000000-0000-4000-8000-000000000001` |
| `comprador2@boheme.test` | COMPRADOR | `c1000000-0000-4000-8000-000000000002` |

Contraseña ficticia local de esas cuatro cuentas: **`BohemeDemo2026!`**.
Puede sustituirse con `BOHEME_DEMO_PASSWORD`. El seed codifica mediante BCrypt
antes de persistir y no sobrescribe cuentas ya existentes. No se crea ninguna
de estas cuentas fuera del perfil dev.

Inicio reproducible con el JDK 21 instalado en este equipo, sin guardar secretos:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\boheme-temurin-21.0.12.1\jdk-21.0.12.1+1"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
$jwtKeyBytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtKeyBytes)
$env:BOHEME_JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
$env:BOHEME_JWT_DURATION = 'PT1H'
.\gradlew.bat bootRun '--args=--spring.profiles.active=dev --server.port=0'
```

`server.port=0` elige un puerto libre. Buscar `Tomcat started on port ...` en el log;
en la verificación fue **57880**. No se asumió 8080 ni 8081 y no se detuvieron las
instancias anteriores. Consola local: `http://localhost:PUERTO/h2-console/`, URL
`jdbc:h2:mem:ecommercedb`, usuario `sa`, contraseña vacía.

Python **no es una dependencia del backend**. El archivo
`scripts/verificar-seguridad-jwt.py` es un cliente externo para reproducir las
pruebas HTTP y recoger evidencia de la consola H2:

```powershell
python scripts/verificar-seguridad-jwt.py --base-url http://localhost:57880
```

El script crea cuatro cuentas ficticias, una prenda que termina suspendida y una
bolsa que termina cerrada. Puede repetirse porque genera nuevos correos únicos.
Espera el perfil dev con las credenciales ficticias predeterminadas y duración
de token de una hora. El verificador histórico de 2.6 refleja la API anterior a
JWT; para la API protegida debe usarse este nuevo verificador.

## Ejemplos de registro, login y respuestas

Registro comprador:

```json
{"nombre":"Cuenta HTTP comprador","correo":"comprador-a105a8b25e@boheme.test","password":"PruebaHttp2026!","rol":"COMPRADOR"}
```

Respuesta real 201:

```json
{
  "id":"3b46c912-5566-467e-b3dd-65bae9824cb4",
  "nombre":"Cuenta HTTP comprador",
  "correo":"comprador-a105a8b25e@boheme.test",
  "rol":"COMPRADOR",
  "activo":true
}
```

Login:

```json
{"correo":"comprador-a105a8b25e@boheme.test","password":"PruebaHttp2026!"}
```

Respuesta real 200, sustituyendo únicamente el JWT completo para no publicarlo:

```json
{
  "token":"[JWT omitido; claims guardados por separado]",
  "tokenType":"Bearer",
  "expiresIn":3600,
  "expiresAt":"2026-10-25T03:32:49Z",
  "rol":"COMPRADOR",
  "usuario":{
    "id":"3b46c912-5566-467e-b3dd-65bae9824cb4",
    "nombre":"Cuenta HTTP comprador",
    "correo":"comprador-a105a8b25e@boheme.test",
    "rol":"COMPRADOR",
    "activo":true
  }
}
```

Claims reales de ese token:

```json
{
  "sub":"3b46c912-5566-467e-b3dd-65bae9824cb4",
  "correo":"comprador-a105a8b25e@boheme.test",
  "iss":"boheme",
  "exp":1792899169,
  "iat":1792895569,
  "jti":"0e4536ba-0689-4703-bc62-976efe68b3c7",
  "rol":"COMPRADOR"
}
```

Para registrar una prenda se inicia sesión con el vendedor y se envía su UUID
`75c5d8e0-52ca-41e9-bfea-113246546bb4` como creadorId, con el JWT en la cabecera.
Para crear una bolsa se usa el UUID del comprador. Los cuerpos, IDs de recursos
y errores reales completos están en la evidencia HTTP; el registro de ejemplos
del informe no publica contraseñas ni claves reales de usuarios.

## Pruebas automatizadas

| Suite nueva | Ejecuciones | Comprobaciones |
|---|---:|---|
| `SeguridadJwtIntegrationTest` | 37 | Filtros reales, H2, registro/login, hash, claims, expiración, firma, cuentas inactivas, rutas/roles, propiedad, errores, consultas públicas y Swagger |
| `UsuarioJpaTest` | 3 | Round trip JPA, estado inactivo y restricción única real incluso sin consulta previa |
| `JwtConfigTest` | 9 | Secretos ausentes/cortos/invalidos, duración inválida y rechazo de contraseña plana en dominio |
| Suites anteriores | 140 | Dominio, casos de uso, REST, persistencia y consultas 2.6 conservadas |
| **Total** | **189** | **0 fallos, 0 errores, 0 omitidas** |

Las pruebas MVC previas conservan sus aserciones y filtros activos. Se importó
`SecurityConfig`, se añadieron mocks de los puertos requeridos y `@WithMockUser`
con el rol de cada controller protegido. No se usó `addFilters=false` ni se
desactivaron pruebas. La suite de seguridad usa JWT firmado real, BCrypt real,
H2 y rollback por prueba, sin mocks de seguridad. Usa AAA.

Las pruebas JPA usan el paquete Boot 4
`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.
El test completo fue ejecutado con Java 21 mediante el comando solicitado.
Informe generado: `build/reports/tests/test/index.html`.
Gradle emitió avisos de API obsoleta e instrumentación de JVM; el build fue exitoso.

## Evidencia HTTP y SQL real

Instancia comprobada: `http://localhost:57880`, perfil dev, con secreto aleatorio
generado para ese proceso. 43 peticiones satisfactorias incluyen:

| Acción | Estado comprobado |
|---|---|
| Registrar cuatro cuentas de ambos roles | 201 |
| Login de cuentas registradas y cuatro demo | 200 |
| Correo duplicado con mayúsculas | 409 |
| Datos inválidos y rol ADMIN | 400 |
| Contraseña incorrecta / correo inexistente | 401 genérico |
| Crear prenda sin JWT / firma alterada | 401 |
| Comprador intentando crear prenda | 403 |
| Vendedor intentando crear bolsa | 403 |
| Cambiar creadorId/clienteId del JSON | 403 |
| Modificar prenda ajena con mismo rol | 403 |
| Consultar/vaciar/cerrar bolsa ajena con mismo rol | 403 |
| Crear prenda propia y bolsa propia | 201 |
| Actualizar stock, suspender, vaciar y cerrar recursos propios | 204 |
| Recurso inexistente y bolsa ya cerrada al consultar activa | 404 |
| Consultas públicas sin JWT | 200 |
| OpenAPI con doce operaciones y Swagger UI disponible | 200 |

Expiración y claims inválidos se comprobaron mediante integración con el filtro
y decoder reales; no se esperó una hora ni se firmaron tokens vencidos mediante
el verificador HTTP externo.

Las tres consultas SQL fueron SELECT reales de la consola de la misma JVM,
usando `jdbc:h2:mem:ecommercedb;IFEXISTS=TRUE`. Se comprobaron:

- Ocho usuarios persistidos, todos con prefijo BCrypt `$2a$12$` y longitud 60.
- Cuatro prendas asociadas a cuentas VENDEDOR, incluidas las tres del seed.
- Tres bolsas asociadas a cuentas COMPRADOR, incluidas las dos del seed.

SQL de evidencia de contraseñas, sin publicar el hash completo:

```sql
SELECT id, correo, rol, activo,
       LEFT(password_hash,7) AS bcrypt_prefijo,
       LENGTH(password_hash) AS hash_longitud
FROM usuarios ORDER BY correo;
```

Propiedad real:

```sql
SELECT p.id, p.creador_id, u.correo, u.rol
FROM prendas_autor p JOIN usuarios u ON u.id=p.creador_id
WHERE p.eliminado=FALSE ORDER BY p.id;

SELECT b.id, b.cliente_id, u.correo, u.rol
FROM bolsas_estilo b JOIN usuarios u ON u.id=b.cliente_id
WHERE b.eliminado=FALSE ORDER BY b.id;
```

## Archivos creados y modificados en 2.7

El repositorio ya contenía modificaciones y archivos sin seguimiento de apartados
anteriores. Este inventario se refiere a lo añadido/tocado en esta implementación,
no a la clasificación global de `git status`.

Archivos Java nuevos, relativos a `src/main/java/com/uniquindio/ecommerce/`:

| Paquete | Archivos |
|---|---|
| `domain/entity` | `Usuario.java` |
| `domain/valueobject` | `Rol.java` |
| `domain/repository` | `UsuarioRepository.java` |
| `domain/exception` | `CorreoDuplicadoException.java` |
| `application/security` | `CodificadorContrasenas.java`, `EmisorTokens.java`, `UsuarioActual.java`, `CredencialesInvalidasException.java`, `AccesoDenegadoException.java` |
| `application/usecase` | `RegistrarUsuarioUseCase.java`, `IniciarSesionUseCase.java`, `AutorizarRecursosUseCase.java` |
| `application/dto/request` | `RegistroRequest.java`, `LoginRequest.java` |
| `application/dto/response` | `UsuarioResponse.java`, `LoginResponse.java` |
| `infrastructure/persistence/jpa/entity` | `UsuarioJpaEntity.java` |
| `infrastructure/persistence/jpa/repository` | `UsuarioJpaRepository.java` |
| `infrastructure/persistence/jpa/mapper` | `UsuarioJpaMapper.java` |
| `infrastructure/persistence/jpa` | `JpaUsuarioRepository.java` |
| `infrastructure/persistence/jpa/seed` | `UsuariosDemo.java` |
| `infrastructure/security` | `BCryptContrasenas.java`, `JwtConfig.java`, `NimbusEmisorTokens.java`, `SecurityContextUsuarioActual.java`, `SecurityConfig.java` |
| `infrastructure/rest/mapper` | `UsuarioMapper.java` |
| `infrastructure/rest/controller` | `AuthController.java` |

Otros archivos nuevos:

- `src/main/resources/application-dev.properties`.
- `src/test/resources/application.properties`.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/security/SeguridadJwtIntegrationTest.java`.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfigTest.java`.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/UsuarioJpaTest.java`.
- `scripts/verificar-seguridad-jwt.py`, este informe y los cinco archivos de evidencia enlazados arriba.

Archivos existentes modificados:

- `build.gradle`: starters de seguridad de ejecución y pruebas.
- `src/main/resources/application.properties`: propiedades JWT; seed y consola desactivados fuera de dev.
- `src/main/java/com/uniquindio/ecommerce/infrastructure/config/OpenApiConfig.java`: Bearer y autorización por operación.
- `src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaController.java`: invocación al caso de autorización en escrituras.
- `src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/BolsaDeEstiloController.java`: autorización de identidad/propiedad.
- `src/main/java/com/uniquindio/ecommerce/infrastructure/rest/exception/ApiExceptionHandler.java`: 401/403/409 de autenticación y autorización.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaControllerTest.java`.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/BolsaDeEstiloControllerTest.java`.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/ReporteControllerTest.java`.

## Tabla de cumplimiento de 2.7

| Requisito | Implementación | Comprobación |
|---|---|---|
| Registro de usuarios | POST register, dos roles permitidos | Integración H2 y HTTP 201/400/409 |
| Login | POST login | Integración y HTTP 200/401 |
| JWT con identidad, correo, rol y vencimiento | sub, correo, rol, exp; firma HS256 | Decoder real y claims HTTP |
| Contraseñas BCrypt | Coste 12, sin plaintext | Pruebas de matches y SELECT de H2 |
| Secreto y duración configurables | application.properties + variables de entorno | Arranque real; pruebas de configuración inválida |
| Roles COMPRADOR y VENDEDOR | Enum cerrado y ROLE_ en Security | Login y matriz de rutas |
| Dos o más rutas por rol | Tres de vendedor y cuatro de comprador | Siete operaciones con 401/403 en integración |
| Token ausente → 401 | Entry point JSON | Pruebas y HTTP |
| Rol incorrecto → 403 | AuthorizationFilter | Pruebas y HTTP |
| Propiedad y no suplantación UUID | Caso de autorización + propietario persistido | Pruebas y HTTP de ambos roles |
| JWT inválido/vencido rechazado | Nimbus decoder y validadores | Firma alterada HTTP; expirado en integración |
| Sin sesiones | SecurityFilterChain stateless | Sin Set-Cookie en HTTP |
| Dominio independiente de Security/JPA | Cuenta y puertos propios | Revisión de imports y pruebas |
| Usuarios persistidos en H2 | Entidad y mapper independientes | DataJpaTest y SQL de misma JVM |
| Datos demo coherentes | Perfil dev y cuatro cuentas con IDs del seed | Cuatro login y JOIN SQL |
| H2 local, no público en producción | Perfil dev, bind loopback, consola apagada por defecto | HTTP en dev; denegación fuera de dev en integración |
| Swagger Bearer y 401/403 | SecurityScheme + customizer por operación | OpenAPI real y test de esquema |
| Authorize y envío desde la UI | Esquema configurado para Swagger UI | **Interacción visual pendiente: herramienta detuvo Chrome** |
| Diez endpoints anteriores más dos auth | Doce operaciones | OpenAPI HTTP y 140 pruebas previas |
| Pruebas sin borrar/desactivar | 49 nuevas, filtros MVC activos | clean test: 189/189 |
| Errores JSON sin información sensible | Handler global reutilizado | 400/401/403/404/409; regresiones 500 previas |

## Límites y pendientes

El único paso solicitado que no pudo comprobarse realmente es la interacción
visual con **Swagger Authorize/Execute**, por el bloqueo de la herramienta de
navegador descrito. Debe revisarse visualmente antes de dar ese paso por validado.

H2 conserva su carácter volátil. El perfil dev y las cuentas ficticias son sólo
para desarrollo local. El modelo conserva las reglas anteriores, incluida la
posibilidad de varias bolsas activas y la independencia del stock total.

No se implementaron refresh tokens, logout/revocación individual, administración
de cuentas ni cambio de contraseñas: no pertenecen al alcance solicitado. Un JWT
vence según su duración; la validación consulta el estado de la cuenta en cada
petición. No se añadieron funcionalidades de 2.8/2.9 ni secretos de producción.
