# BOHEME

Marketplace de ropa de autor desarrollado para **Programación Avanzada de la Universidad del Quindío**.

BOHEME conecta compradores con creadores de talleres y marcas independientes. Busca facilitar la publicación y consulta de prendas originales y convertir una selección por tallas en un pedido con precios e inventario coherentes.

El **Entregable 2 implementa el backend**, desde la API REST y la persistencia hasta la autenticación y el flujo completo de compra. El frontend corresponde a la siguiente entrega. La confirmación registra un pedido en estado **PENDIENTE**; no realiza un cobro mediante una pasarela de pagos.

**Verificación del 25 de octubre de 2026:** `clean test` ejecutado nuevamente, **219 pruebas exitosas en 21 clases**, sin fallos, errores ni omisiones. Se contrastaron **15 operaciones REST** con los controllers y OpenAPI.

## Índice

- [Integrantes](#integrantes)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Funcionalidades](#funcionalidades)
- [Correcciones de la Entrega 1](#correcciones-de-la-entrega-1)
- [Requisitos previos](#requisitos-previos)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Configuración](#configuración)
- [Usuarios de demostración](#usuarios-de-demostración)
- [API REST](#api-rest)
- [Seguridad](#seguridad)
- [Persistencia y consultas](#persistencia-y-consultas)
- [Pruebas automatizadas](#pruebas-automatizadas)
- [Pruebas avanzadas](#pruebas-avanzadas)
- [Flujo completo de compra](#flujo-completo-de-compra)
- [Limitaciones y trabajo futuro](#limitaciones-y-trabajo-futuro)
- [Estado de Git y entrega académica](#estado-de-git-y-entrega-académica)
- [Documentación adicional](#documentación-adicional)

## Integrantes

| Integrante | Responsabilidad |
| --- | --- |
| [PENDIENTE] | [PENDIENTE] |

Completar con nombres y responsabilidades acordados por el equipo. No se encontró un listado confirmado de integrantes y aportes; los mensajes de commits no bastan para atribuir responsabilidades completas.

## Tecnologías

Versiones declaradas en [build.gradle](build.gradle), en el [Gradle Wrapper](gradle/wrapper/gradle-wrapper.properties) o comprobadas mediante `dependencies --configuration testRuntimeClasspath`. Las versiones transitivas son las resueltas en esta revisión, no recomendaciones de actualización.

| Tecnología | Versión o configuración real | Función | Motivo de utilización |
| --- | --- | --- | --- |
| Java | 21, toolchain del proyecto | Lenguaje y plataforma | Tipado, records y modelado del dominio |
| Spring Boot | 4.1.1 | Arranque y autoconfiguración | Integrar MVC, seguridad y persistencia |
| Gradle Wrapper | 9.7.1 | Compilación, dependencias y pruebas | Construcción reproducible sin Gradle global |
| Jakarta Persistence | 3.2.0 | API JPA | Mapear entidades y relaciones |
| Spring Data JPA | 4.1.1 | Repositorios y consultas | Adaptar puertos del dominio a acceso relacional |
| Hibernate ORM | 7.4.5.Final | Implementación JPA | Relaciones, transacciones, bloqueos y soft delete |
| H2 | 2.4.240 | Base en memoria | Demostración local y pruebas aisladas |
| Spring Security | 7.1.1 | Filtros de autenticación y autorización | Validar Bearer y restringir roles |
| JWT | JWS con HS256, mediante Nimbus/Spring Security | Token de identidad firmado | Autenticar peticiones sin sesión HTTP |
| BCrypt | Coste 12, Spring Security Crypto 7.1.1 | Hash de contraseñas | Evitar almacenamiento de contraseñas en texto plano |
| springdoc OpenAPI | 3.0.2 | Generación del contrato OpenAPI | Documentar entradas, respuestas y seguridad |
| Swagger UI | 5.32.0, dependencia de springdoc | Cliente interactivo | Ejecutar la demostración de la API |
| JUnit Jupiter y Platform | 6.0.3 | Ejecución y aserciones | Probar reglas, contratos e integración |
| Mockito | 5.23.0 | Dobles de prueba | Aislar colaboradores y comprobar delegación |

El driver MariaDB está declarado en Gradle, pero **la conexión actual utiliza H2**. Su presencia no significa que la migración esté implementada.

## Arquitectura

El enfoque **Domain-Driven Design (DDD)** sitúa las reglas en el modelo de negocio y separa responsabilidades:

| Capa | Contenido | Responsabilidad |
| --- | --- | --- |
| `domain` | Entidades, Value Objects, excepciones, puertos de repositorio, servicios y criterios de consulta | Proteger reglas, identidad y límites de agregados |
| `application` | Casos de uso, DTO y puertos de seguridad | Coordinar operaciones, transacciones y resultados |
| `infrastructure` | Controllers, mappers REST/JPA, adaptadores, configuración y seguridad | Adaptar HTTP, Spring, autenticación y almacenamiento |

~~~mermaid
flowchart LR
    H[HTTP] --> C[Controller]
    C --> M[DTO y Mapper]
    M --> U[Caso de uso]
    U --> D[Dominio]
    U --> R[Repositorio del dominio]
    R -. implementado por .-> A[Adaptador JPA]
    A --> B[(H2)]
~~~

El repositorio es un **contrato**; el adaptador implementa ese contrato usando Spring Data JPA y mappers de persistencia. La flecha hacia el adaptador representa la implementación en ejecución, no una dependencia del dominio hacia infraestructura.

El paquete `domain` **no importa Spring, JPA ni HTTP**. Los casos de uso sí utilizan `@Service` y `@Transactional`. Por ejemplo, `PrendaDeAutor` valida una prenda y `BolsaDeEstilo` controla sus líneas, mientras `ConfirmarPedidoUseCase` coordina bolsa, inventario y pedido. Las entidades JPA se mantienen separadas del dominio.

Rutas principales:

~~~text
src/main/java/com/uniquindio/ecommerce/
  domain/
  application/
  infrastructure/
src/main/resources/
src/test/java/com/uniquindio/ecommerce/
docs/
scripts/
~~~

## Funcionalidades

| Área | Implementado | Alcance y límites |
| --- | --- | --- |
| Acceso | Registro, login, JWT y BCrypt | Roles COMPRADOR/VENDEDOR; sin refresh ni revocación individual |
| Prendas | Publicar con variantes, listar, suspender venta y actualizar stock por talla | No hay edición HTTP general ni endpoint para borrar prendas |
| Catálogo | Búsqueda textual, filtros, paginación y ranking | El listado incluye suspendidas; el ranking cuenta oferta, no ventas |
| Bolsas | Crear, consultar activa, agregar variantes, vaciar y cerrar | Se permiten varias activas; agregar no reserva inventario |
| Pedidos | Confirmar compra y consultar pedido propio | Estado inicial PENDIENTE, sin cobro real |
| Inventario | Revalidar y descontar stock por variante al confirmar | `stockTotal` sigue siendo independiente del stock por talla |
| Consistencia | Transacción, rollback, bloqueo de raíces y un pedido por bolsa | Pruebas de concurrencia realizadas sobre H2 |
| Autorización | Restricciones por rol, identidad y propietario | Un UUID recibido no concede acceso a un recurso ajeno |
| Eliminación lógica | Soft delete de prendas, variantes, bolsas y líneas de bolsa | Vaciar muestra eliminación de líneas; suspender/cerrar no son borrados |

Los precios y subtotales del pedido son históricos. Cambiar o eliminar lógicamente el catálogo no altera el pedido registrado.

## Correcciones de la Entrega 1

Las observaciones conocidas de la profesora fueron **constructores privados** y **validaciones e invariantes**. Los constructores de las siete entidades originales **ya eran privados**: se verificaron y se incorporaron pruebas, sin atribuirles una modificación inexistente.

Las rutas abreviadas de esta tabla son relativas a [src/main/java/com/uniquindio/ecommerce](src/main/java/com/uniquindio/ecommerce). Las pruebas pertenecen a [InvariantesEntidadesTest](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java).

| Observación de la profesora | Corrección realizada | Archivo o método | Evidencia |
| --- | --- | --- | --- |
| Constructores privados | Verificar las siete entidades originales y sus fábricas públicas estáticas | `domain/entity`; constructores y `crear` | `entidades_SoloExponenConstructoresPrivadosYFabricaCrear` comprueba por reflexión |
| Validaciones e invariantes | Reforzar IDs, nombre, precio positivo, stock y variantes válidas; mutaciones mediante métodos validados | `PrendaDeAutor.actualizarStock` y `actualizarStockVariante` | `prenda_ActualizaStockValidoYRechazaNegativoSinMutar`; `prenda_RechazaActualizacionesDeVarianteInvalidasSinMutar` |
| Validaciones e invariantes | Validar estado, moneda y cantidad acumulada sin desbordamiento | `BolsaDeEstilo.agregarLinea` | `bolsa_RechazaEntradasInvalidasYMezclaDeMonedasSinMutar`; `bolsa_CantidadAcumuladaNoDesbordaEntero` |
| Validaciones e invariantes | Copia interna modificable, salida protegida y prohibición de eliminar la última vitrina | `Creador.eliminarVitrina` y `getVitrinas` | `creador_EncapsulaVitrinasYConservaAlMenosUnaAlEliminar` |
| Validaciones e invariantes | Copiar catálogo, rechazar nulos y corregir comparación UUID al eliminar prenda | `VitrinaDeCreador.eliminarPrenda` | `vitrina_EncapsulaCatalogoYPermiteOperacionesValidas` |

La prueba de constructores enumera `PrendaDeAutor`, `VariantePrenda`, `VarianteDeTalla`, `BolsaDeEstilo`, `PedidoDeModa`, `Creador` y `VitrinaDeCreador`. No incluye `Usuario`, añadido después.

**Evidencia Git pendiente:** las correcciones recientes siguen en cambios locales; no hay un commit nuevo verificado que pueda citarse para ellas. Puede inspeccionarse el antes/después con:

~~~powershell
git diff -- src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java
git diff -- src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java
~~~

## Requisitos previos

- **JDK 21** instalado y accesible mediante `JAVA_HOME`.
- **Windows PowerShell o PowerShell 7** para los comandos siguientes.
- **Git**, si se va a clonar el repositorio.
- Conexión a internet para descargar inicialmente la distribución de Gradle y las dependencias.
- Un navegador para Swagger UI, H2 Console y el reporte de pruebas.

El repositorio incluye **Gradle Wrapper**: no es obligatorio instalar Gradle globalmente. Tampoco se necesita instalar H2/MariaDB como servidor ni Node.js/Python para ejecutar el backend.

## Instalación y ejecución

### 1. Clonar y entrar al proyecto

El remoto configurado y verificado en Git es:

~~~powershell
git clone https://github.com/sofism01/ecommerce-PA.git
Set-Location ecommerce-PA
~~~

**Estado de publicación:** existen avances locales sin commit/push. Hasta que el equipo los publique, clonar ese remoto puede recuperar una versión anterior al backend descrito aquí. Este README prepara la publicación y no afirma que todos los cambios ya estén en GitHub.

### 2. Seleccionar Java 21

Desde la raíz del proyecto, escribir la ruta del **JDK 21 instalado en tu equipo**, sin comillas adicionales cuando el comando la solicite:

~~~powershell
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = Read-Host 'Ruta de tu JDK 21 instalado, sin comillas'
if (!(Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    throw 'La ruta no contiene bin\java.exe. Selecciona el directorio del JDK 21.'
}
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
java -version
.\gradlew.bat --version
~~~

Comprobar que Java indica **21** y que el Wrapper utiliza ese JDK. El valor de `JAVA_HOME` y el Java del `PATH` pueden ser diferentes; por eso se ajustan ambos en esta terminal.

### 3. Ejecutar compilación y pruebas

~~~powershell
.\gradlew.bat clean test
~~~

Esperar **BUILD SUCCESSFUL**. El reporte está en `build/reports/tests/test/index.html`, generado localmente. Para abrirlo:

~~~powershell
Start-Process '.\build\reports\tests\test\index.html'
~~~

La configuración de pruebas tiene una clave ficticia propia: este comando no requiere que definas el secreto de ejecución de la aplicación.

### 4. Configurar JWT y arrancar Spring Boot

En la misma terminal, generar un secreto temporal de 48 bytes aleatorios. El comando lo mantiene en el entorno del proceso y **no imprime su valor**:

~~~powershell
$bohemeJwtBytes = New-Object byte[] 48
$bohemeJwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bohemeJwtRng.GetBytes($bohemeJwtBytes)
$bohemeJwtRng.Dispose()
$env:BOHEME_JWT_SECRET = [Convert]::ToBase64String($bohemeJwtBytes)
$env:BOHEME_JWT_DURATION = 'PT1H'
$env:BOHEME_DEMO_PASSWORD = 'BohemeDemo2026!'
.\gradlew.bat bootRun '--args=--spring.profiles.active=dev --server.port=0 --spring.jpa.show-sql=false' --console=plain
~~~

`dev` habilita el conjunto ficticio y la consola H2 local. `server.port=0` solicita un puerto libre; no significa que debas abrir el puerto cero. El argumento de SQL reduce ruido de logs, sin cambiar archivos de configuración.

No publiques el valor de `BOHEME_JWT_SECRET`, no lo guardes en Git ni lo pegues en servicios externos. Fuera del ensayo local, proporciona una clave aleatoria mediante la gestión de secretos del entorno. Volver a generar la clave invalida los JWT firmados con la anterior.

### 5. Encontrar las URL correctas

Esperar `Started EcommerceApplication` y buscar **`Tomcat started on port NNNNN`**. Sustituir `PUERTO` por ese número:

| Recurso | URL |
| --- | --- |
| Swagger UI | `http://127.0.0.1:PUERTO/swagger-ui/index.html` |
| OpenAPI JSON | `http://127.0.0.1:PUERTO/api-docs` |
| H2 Console, sólo dev | `http://127.0.0.1:PUERTO/h2-console/` |

Por ejemplo, si el log indica 54321, Swagger será `http://127.0.0.1:54321/swagger-ui/index.html`. **No se presupone 8080 ni un puerto de una sesión anterior.**

Mantener la terminal abierta. `Ctrl+C` detiene esta instancia; la base H2 desaparece. Para repetir la demostración desde los datos iniciales, arrancar de nuevo y copiar el puerto y los IDs de esa nueva ejecución.

### 6. Conectarse a H2 Console

Abrir la URL de H2 del mismo puerto y usar:

| Campo | Valor |
| --- | --- |
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:ecommercedb;IFEXISTS=TRUE` |
| User Name | `sa` |
| Password | Vacío |

`IFEXISTS=TRUE` evita crear una base por un error de nombre. La consola debe pertenecer a la **misma JVM de Spring**: una H2 abierta desde otro proceso no comparte esta base en memoria. Tras reiniciar, reconectar. Ver las tablas del esquema `PUBLIC`.

Si falla el arranque, revisar Java 21, secreto Base64 con al menos 32 bytes y descarga de dependencias. Si H2 devuelve 403, revisar que se activó `dev`; si no encuentra la base, revisar puerto y JDBC URL. Para errores JWT, volver a hacer login y reemplazar la autorización de Swagger.

## Configuración

Fuentes: [application.properties](src/main/resources/application.properties) y [application-dev.properties](src/main/resources/application-dev.properties).

| Propiedad o variable | Valor o comportamiento |
| --- | --- |
| `spring.datasource.url` | `jdbc:h2:mem:ecommercedb` |
| Usuario/contraseña H2 | `sa` / vacío, exclusivamente entorno local actual |
| `spring.jpa.hibernate.ddl-auto` | `create-drop`; crea y retira el esquema |
| `spring.jpa.open-in-view` | `false`; las conversiones se completan dentro de transacciones |
| `spring.jpa.show-sql` | `true` por defecto; el arranque propuesto lo desactiva por argumento |
| `springdoc.api-docs.path` | `/api-docs` |
| `springdoc.swagger-ui.path` | `/swagger-ui.html`; UI también accesible en `/swagger-ui/index.html` |
| `BOHEME_JWT_SECRET` | Obligatorio al arrancar la aplicación: Base64 de al menos 32 bytes aleatorios, sin clave predeterminada |
| `BOHEME_JWT_DURATION` | `PT1H` por defecto; validación entre 1 segundo y 24 horas |
| `boheme.datos-iniciales.enabled` | `false` fuera de dev y `true` en dev |
| Consola H2 y cuentas demo | Habilitadas sólo en dev; consola apagada y denegada fuera de ese perfil |
| `server.address` en dev | `127.0.0.1`; ejecución local |
| `BOHEME_DEMO_PASSWORD` | Reemplaza la contraseña ficticia del seed dev; por defecto `BohemeDemo2026!` |

**Los datos H2 no sobreviven al reinicio.** Las propiedades de pruebas son independientes de la base de demostración y no deben copiarse a producción.

## Usuarios de demostración

Cuentas verificadas en [UsuariosDemo.run](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/seed/UsuariosDemo.java). Los cuatro logins se comprobaron con HTTP 200 durante esta revisión.

| Correo | Contraseña ficticia de demostración | Rol |
| --- | --- | --- |
| `vendedor1@boheme.test` | `BohemeDemo2026!` | VENDEDOR |
| `vendedor2@boheme.test` | `BohemeDemo2026!` | VENDEDOR |
| `comprador1@boheme.test` | `BohemeDemo2026!` | COMPRADOR |
| `comprador2@boheme.test` | `BohemeDemo2026!` | COMPRADOR |

Son credenciales **ficticias, locales y exclusivamente académicas**; no deben utilizarse en producción. Sólo existen con el perfil dev y el inicializador habilitado. Si se cambia `BOHEME_DEMO_PASSWORD` antes del arranque, utilizar ese nuevo valor.

Para el recorrido principal se recomienda **vendedor1 y comprador2**. Comprador1 tiene inicialmente una bolsa activa con artículos; comprador2 tiene una bolsa precargada cerrada y puede crear una nueva sin esa selección previa.

## API REST

Las **15 operaciones** se agrupan a continuación. “Propio” exige tanto rol como propiedad del recurso. Los GET de catálogo/ranking y registro/login son públicos. JSON usa `Content-Type: application/json`.

### Autenticación

| Método | Ruta | Descripción | Rol requerido | Éxito |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/register` | Registrar cuenta | Público | 201 |
| POST | `/api/auth/login` | Autenticar y emitir JWT | Público | 200 |

### Prendas

| Método | Ruta | Descripción | Rol requerido | Éxito |
| --- | --- | --- | --- | --- |
| POST | `/api/prendas` | Publicar prenda y variantes | VENDEDOR, identidad propia | 201 |
| GET | `/api/prendas` | Buscar con filtros y paginación | Público | 200 |
| GET | `/api/prendas/creador/{creadorId}` | Listar prendas de un creador | Público | 200 |
| PATCH | `/api/prendas/{prendaId}/suspender` | Suspender venta | VENDEDOR propietario | 204 |
| PATCH | `/api/prendas/{prendaId}/variantes/{varianteId}/stock` | Fijar stock absoluto de variante | VENDEDOR propietario | 204 |

### Bolsas

| Método | Ruta | Descripción | Rol requerido | Éxito |
| --- | --- | --- | --- | --- |
| POST | `/api/bolsas` | Crear bolsa vacía | COMPRADOR, identidad propia | 201 |
| GET | `/api/bolsas/activas/{clienteId}` | Consultar una bolsa activa | COMPRADOR propietario | 200 |
| POST | `/api/bolsas/{bolsaId}/items` | Agregar o acumular una variante | COMPRADOR propietario | 200 |
| DELETE | `/api/bolsas/{bolsaId}/items` | Vaciar bolsa activa | COMPRADOR propietario | 204 |
| PATCH | `/api/bolsas/{bolsaId}/cerrar` | Cerrar manualmente, sin compra | COMPRADOR propietario | 204 |
| POST | `/api/bolsas/{bolsaId}/confirmar-pedido` | Confirmar compra transaccional | COMPRADOR propietario | 201 |

### Pedidos

| Método | Ruta | Descripción | Rol requerido | Éxito |
| --- | --- | --- | --- | --- |
| GET | `/api/pedidos/{pedidoId}` | Consultar histórico de pedido | COMPRADOR propietario | 200 |

### Reportes

| Método | Ruta | Descripción | Rol requerido | Éxito |
| --- | --- | --- | --- | --- |
| GET | `/api/reportes/creadores/variedad` | Ranking por variantes/prendas ofertadas | Público | 200 |

Las respuestas 204 no contienen cuerpo. UUID o DTO inválidos producen 400; ausencia de recurso, 404; reglas de negocio o conflictos, 409. El contrato de error incluye `timestamp`, `status`, `code`, `message`, `path` y `details`. Un error inesperado devuelve 500 genérico, sin trazas internas al cliente.

### Ejemplos de solicitudes y respuestas

Los siguientes JSON muestran **formatos ilustrativos**, no capturas de respuestas ejecutadas. Los UUID de recursos recién creados deben sustituirse por los devueltos por tu instancia. No se incluye ningún JWT operativo.

**Registro — POST /api/auth/register → 201**

~~~json
{
  "nombre": "Comprador de ensayo",
  "correo": "ensayo@boheme.test",
  "password": "BohemeDemo2026!",
  "rol": "COMPRADOR"
}
~~~

Respuesta de ejemplo:

~~~json
{
  "id": "d1000000-0000-4000-8000-000000000001",
  "nombre": "Comprador de ensayo",
  "correo": "ensayo@boheme.test",
  "rol": "COMPRADOR",
  "activo": true
}
~~~

El correo debe ser nuevo; repetirlo produce 409 `CORREO_DUPLICADO`. Registro requiere al menos 12 caracteres y máximo 72 bytes UTF-8 en la contraseña.

**Login — POST /api/auth/login → 200**

~~~json
{"correo":"vendedor1@boheme.test","password":"BohemeDemo2026!"}
~~~

Respuesta de ejemplo, con token omitido y fechas ilustrativas:

~~~json
{
  "token": "TOKEN_OMITIDO_EN_DOCUMENTACION",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "expiresAt": "2026-10-25T22:00:00Z",
  "rol": "VENDEDOR",
  "usuario": {
    "id": "81000000-0000-4000-8000-000000000001",
    "nombre": "Cuenta demo VENDEDOR",
    "correo": "vendedor1@boheme.test",
    "rol": "VENDEDOR",
    "activo": true
  }
}
~~~

**Publicación — POST /api/prendas → 201**, con JWT de vendedor1:

~~~json
{
  "creadorId": "81000000-0000-4000-8000-000000000001",
  "nombre": "Camisa de ensayo",
  "descripcion": "Camisa de lino de autor",
  "precioBase": 19.95,
  "moneda": "COP",
  "stockTotal": 3,
  "variantes": [
    {"sku":"ENSAYO-M","tipoTalla":"M","stockDisponible":3}
  ]
}
~~~

Respuesta de ejemplo:

~~~json
{
  "prendaId": "d2000000-0000-4000-8000-000000000001",
  "creadorId": "81000000-0000-4000-8000-000000000001",
  "nombre": "Camisa de ensayo",
  "descripcion": "Camisa de lino de autor",
  "precioBase": {"monto":19.95,"moneda":"COP"},
  "variantes": [
    {
      "varianteId": "d3000000-0000-4000-8000-000000000001",
      "sku": "ENSAYO-M",
      "tipoTalla": "M",
      "stockDisponible": 3
    }
  ],
  "stockTotal": 3,
  "disponibleParaLaVenta": true
}
~~~

Copiar `prendaId` y `variantes[0].varianteId` reales. Para otra cuenta, `creadorId` debe coincidir con `usuario.id` de su login.

**Confirmación — POST /api/bolsas/{bolsaId}/confirmar-pedido → 201**, con JWT del comprador dueño y bolsa activa con dos unidades. **No lleva cuerpo JSON**. Respuesta ilustrativa:

~~~json
{
  "pedidoId": "d5000000-0000-4000-8000-000000000001",
  "bolsaId": "d4000000-0000-4000-8000-000000000001",
  "clienteId": "c1000000-0000-4000-8000-000000000002",
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-10-25T21:00:00",
  "lineas": [
    {
      "prendaId": "d2000000-0000-4000-8000-000000000001",
      "varianteId": "d3000000-0000-4000-8000-000000000001",
      "nombrePrenda": "Camisa de ensayo",
      "sku": "ENSAYO-M",
      "tipoTalla": "M",
      "cantidad": 2,
      "precioUnitario": {"monto":19.95,"moneda":"COP"},
      "subtotal": {"monto":39.90,"moneda":"COP"}
    }
  ],
  "total": {"monto":39.90,"moneda":"COP"},
  "moneda": "COP"
}
~~~

La cabecera `Location` indica `/api/pedidos/{pedidoId}`. La compra conserva precios de la bolsa, descuenta stock de la variante y cierra la bolsa dentro de una transacción.

## Seguridad

- **JWT como Bearer Token:** enviar `Authorization: Bearer <token>` en rutas protegidas. Nimbus firma/verifica HS256; se validan emisor, identidad y vencimiento. El payload es legible, no cifrado, y no contiene contraseña ni hash.
- **BCrypt:** almacena un hash con sal y coste 12. Login usa `matches`; no recupera una contraseña original.
- **Stateless:** no se mantiene una sesión HTTP de usuario. En cada petición se consulta la cuenta persistida para verificar actividad, rol y correo.
- **Roles y propiedad:** VENDEDOR opera sus prendas; COMPRADOR, sus bolsas y pedidos. Cambiar un UUID en JSON no permite suplantar al propietario.
- **401 NO_AUTENTICADO:** token ausente, inválido o vencido, o credenciales incorrectas.
- **403 ACCESO_DENEGADO:** autenticación válida con rol insuficiente o recurso ajeno.

En Swagger: ejecutar login, copiar **sólo `token`**, pulsar **Authorize**, pegarlo en `bearerAuth` **sin escribir Bearer**, pulsar Authorize y cerrar. Para cambiar de cuenta: Authorize → Logout → nuevo token → Authorize → Close. Login no cambia automáticamente esa autorización y Logout de Swagger no revoca el JWT en el servidor.

Implementación: [SecurityConfig](src/main/java/com/uniquindio/ecommerce/infrastructure/security/SecurityConfig.java), [JwtConfig](src/main/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfig.java), [BCryptContrasenas](src/main/java/com/uniquindio/ecommerce/infrastructure/security/BCryptContrasenas.java) y [AutorizarRecursosUseCase](src/main/java/com/uniquindio/ecommerce/application/usecase/AutorizarRecursosUseCase.java).

## Persistencia y consultas

Las entidades del dominio protegen reglas; las JPA representan tablas, columnas, relaciones y versiones. Los mappers reconstruyen agregados validados. `DineroJpa` es embebido y conserva monto, moneda y escala; el dominio calcula con `BigDecimal`.

| Tablas | Relación y contenido |
| --- | --- |
| `prendas_autor` → `variantes_prenda` | Una prenda tiene variantes; FK `prenda_id` |
| `bolsas_estilo` → `lineas_bolsa` | Una bolsa contiene selecciones; FK `bolsa_id` |
| `pedidos_moda` → `lineas_pedido` | Un pedido contiene artículos históricos; FK `pedido_id` |
| `usuarios` | Correo único, rol, estado y hash BCrypt |

`creador_id`, `cliente_id` y referencias de catálogo del histórico son UUID escalares; no todas las referencias entre agregados son FK JPA. `pedidos_moda.bolsa_id` tiene una restricción única para impedir dos pedidos por bolsa.

En dev, [DatosIniciales](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/seed/DatosIniciales.java) crea tres prendas, cinco variantes, dos bolsas y tres líneas; `UsuariosDemo` añade cuatro cuentas. Los IDs son estables y el inicializador no duplica ni resucita raíces eliminadas.

**Soft delete:** `@SoftDelete` marca `eliminado=true` y Hibernate excluye esas filas de consultas ORM. Se aplica a prendas, variantes, bolsas y líneas de bolsa; no a usuarios ni pedidos. Vaciar una bolsa marca sus líneas como eliminadas y conserva la raíz. No existe endpoint para borrar raíces; esa capacidad de infraestructura se comprueba con pruebas JPA.

**Listado paginado — GET /api/prendas:** `q` busca en nombre/descripción sin distinguir mayúsculas; `creadorId` y `moneda` añaden filtros con AND. `page` comienza en 0, `size` es de 1 a 100 y vale 20 por defecto. Se ordena por nombre e ID; sin coincidencias devuelve 200 con `content: []` y metadatos. Ejemplo de parámetros:

~~~text
/api/prendas?q=lino&moneda=COP&page=0&size=2
~~~

[PrendaJpaRepository](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/PrendaJpaRepository.java) utiliza JPQL con parámetros nombrados: pagina IDs en SQL y después carga las variantes de esa página. No filtra ni pagina el catálogo completo con listas Java.

**Ranking — GET /api/reportes/creadores/variedad:** JOIN de prendas/variantes, GROUP BY creador y COUNT de variantes más COUNT DISTINCT de prendas. Cuenta oferta vigente, incluyendo variantes de stock cero; excluye prendas suspendidas y filas eliminadas. **Representa variedad por creador, no ventas realizadas.**

## Pruebas automatizadas

Ejecutar desde la raíz, con Java 21:

~~~powershell
.\gradlew.bat clean test
~~~

**Última ejecución verificada: 25 de octubre de 2026.** Se ejecutó `.\gradlew.bat clean test --console=plain`, con **BUILD SUCCESSFUL**, 219 ejecuciones en 21 clases y **0 fallos, 0 errores y 0 omitidas**. `--console=plain` sólo controla el formato del log. Los conteos se extrajeron de los XML nuevos generados por esta ejecución.

| Categoría exclusiva | Ejecuciones |
| --- | --- |
| Dominio | 38 |
| Casos de uso unitarios | 11 |
| Controllers con `@WebMvcTest` | 57 |
| Persistencia con `@DataJpaTest` | 34 |
| Configuración de seguridad unitaria | 9 |
| Integración con `@SpringBootTest` | 70 |
| **Total** | **219** |

Se usa **AAA**: preparar datos y colaboradores, ejecutar la acción y comprobar resultado/efectos. Mockito aísla delegación; los repositorios en memoria prueban coordinación; H2, MockMvc y filtros reales verifican persistencia y seguridad en integración. Los casos parametrizados cuentan por ejecución: 219 no equivale a 219 métodos ni a un porcentaje de cobertura.

- Reporte HTML generado: `build/reports/tests/test/index.html`.
- XML generados: `build/test-results/test/TEST-*.xml`.
- Fuentes: [src/test/java/com/uniquindio/ecommerce](src/test/java/com/uniquindio/ecommerce).
- Evidencia histórica permanente de 2.9: [pruebas.json](docs/evidencias-2.9/pruebas.json) y [log de aquella ejecución](docs/evidencias-2.9/gradle-clean-test.log). Esos archivos no se sobrescribieron con la nueva corrida.

El reporte `build/` no se versiona: se genera al ejecutar las pruebas. Hay verificaciones de rollback después de `flush` real y de confirmaciones concurrentes en [FlujoCompraIntegrationTest](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java). Son pruebas existentes del flujo de compra; aquí no se desarrolla el trabajo académico independiente de seguimiento de pruebas avanzadas.

## Flujo completo de compra

Realizar desde Swagger con los JSON anteriores y copiar siempre IDs de respuestas reales:

1. **Login vendedor1:** POST `/api/auth/login`; copiar token y `usuario.id` y autorizarlo.
2. **Publicar prenda:** POST `/api/prendas` con `creadorId` del vendedor; copiar `prendaId` y `variantes[0].varianteId`. Esperar 201.
3. **Login comprador2:** obtener otro token y cambiar Authorize; su `usuario.id` es `c1000000-0000-4000-8000-000000000002`.
4. **Crear bolsa:** POST `/api/bolsas` con el cuerpo siguiente; copiar `bolsaId` y esperar 201.

~~~json
{"clienteId":"c1000000-0000-4000-8000-000000000002"}
~~~

5. **Agregar variante:** POST `/api/bolsas/{bolsaId}/items`. Reemplazar `VARIANTE_ID_COPIADO` por el `varianteId` de la publicación, manteniendo comillas. Esperar 200, cantidad 2 y subtotal 39,90 COP.

~~~json
{"varianteId":"VARIANTE_ID_COPIADO","cantidad":2}
~~~

6. **Confirmar:** POST `/api/bolsas/{bolsaId}/confirmar-pedido`, sin cuerpo. Esperar 201; copiar `pedidoId`. **No cerrar manualmente la bolsa antes:** el cierre no es una compra e impide confirmar.
7. **Consultar:** GET `/api/pedidos/{pedidoId}` con el token del comprador propietario. Esperar 200 y estado PENDIENTE, artículos y precios históricos.
8. **Verificar inventario:** GET `/api/prendas/creador/{creadorId}`. La variante de stock 3 queda en **1** tras comprar dos unidades.

**Error intencional:** antes de la adición válida, intentar cantidad **4** de esa variante con stock 3. Debe responder **409 STOCK_INSUFICIENTE**, sin línea, pedido ni descuento. Agregar no reserva stock y confirmar lo revalida. Repetir la confirmación de una misma bolsa responde **409 BOLSA_CERRADA**, sin segundo descuento.

[ConfirmarPedidoUseCase](src/main/java/com/uniquindio/ecommerce/application/usecase/ConfirmarPedidoUseCase.java) bloquea bolsa y prendas, ordena los bloqueos por UUID y guarda stock, cierre y pedido dentro de una transacción. Un fallo revierte todo el conjunto. La restricción única por bolsa refuerza la protección contra duplicados. No existe una pasarela de pagos real.

## Limitaciones y trabajo futuro

- Frontend pendiente para la siguiente entrega; la interfaz actual de demostración es Swagger.
- H2 en memoria y `create-drop`: no hay durabilidad entre reinicios. La migración a MariaDB sigue pendiente.
- Sin cobro real, envíos ni reembolsos. Las transiciones antiguas de pago presentes en dominio no constituyen integración HTTP con pagos.
- Sin refresh tokens, revocación individual ni gestión HTTP completa de cuentas.
- Varias bolsas activas por comprador: el adaptador JPA devuelve la primera por ID ascendente, no necesariamente la recién creada. Confirmar usa el `bolsaId` explícito recibido al crearla.
- `stockTotal` es independiente de las existencias de variantes; el descuento de compra afecta a estas últimas.
- Sin endpoints propios para perfiles de creador/vitrina ni eliminación de raíces; conviven clases del modelo inicial que no participan en el recorrido REST actual.
- Las pruebas de concurrencia verifican escenarios sobre H2; no representan una prueba de carga ni certifican otro motor.

## Estado de Git y entrega académica

Al revisar el repositorio, `main` y la referencia local `origin/main` apuntan a **`9230eb1`**, del 17 de octubre de 2026. Los avances recientes y este README están pendientes de registro/publicación. No se han verificado PR ni permisos remotos, y no se atribuyen contribuciones sin confirmación.

Pendiente para cerrar la presentación académica:

- Completar integrantes y responsabilidades.
- Registrar/publicar los cambios conforme al acuerdo del equipo y conservar evidencia real de colaboración.
- Comprobar acceso de la profesora al repositorio y al video.
- Realizar la verificación **visual** de Authorize/Execute y grabar la sustentación. Las comprobaciones HTTP/OpenAPI y automatizadas no se presentan como clics ya realizados en Swagger.

## Documentación adicional

| Documento | Contenido |
| --- | --- |
| [Informe completo del Entregable 2](INFORME_COMPLETO_ENTREGABLE_2.md) | Desarrollo técnico, trazabilidad, decisiones y matriz de rúbrica |
| [Mapeo de API 2.4 a 2.9](docs/api/mapeo-dominio.api.md) | Contratos de las quince operaciones |
| [Persistencia 2.5](docs/persistencia-jpa.md) | Diseño inicial JPA/H2 y eliminación lógica |
| [Consultas avanzadas 2.6](docs/consultas-avanzadas-2.6.md) | JPQL, paginación, ranking y evidencias |
| [Seguridad 2.7](docs/seguridad-jwt-2.7.md) | JWT, BCrypt, roles y propiedad |
| [Pruebas 2.8](docs/pruebas-automatizadas-2.8.md) | Clasificación, AAA y resultados de aquella etapa |
| [Flujo completo 2.9](docs/flujo-negocio-2.9.md) | Compra, transacciones, concurrencia y evidencia HTTP/SQL |
| [Diagrama del agregado inicial](docs/diagrama-agregado.md) | Documentación del modelo de la Entrega 1 |
| [Evidencia SQL de pedido e inventario](docs/evidencias-2.9/final.json) | Resultados históricos de una ejecución demostrada |

Los informes de cada etapa conservan su contexto histórico: por ejemplo, 2.8 reporta 189 pruebas y el estado final incorpora las 30 ejecuciones añadidas en 2.9. Para el inventario actual de operaciones y ejecución, usar este README y el código vigente.

## Pruebas avanzadas

## Pruebas avanzadas

Esta sección corresponde a la actividad **Seguimiento: Pruebas avanzadas del backend**, de la asignatura Programación Avanzada de la Universidad del Quindío, con la docente **Valentina Oviedo Sánchez**.

Para realizar esta actividad revisamos las pruebas que ya teníamos en BOHEME y utilizamos las que cumplían con los requisitos. También agregamos cinco pruebas nuevas para completar los casos de seguridad y los errores del controller.

El seguimiento tiene cinco puntos, del 0 al 4. Para comprobarlos seleccionamos **8 pruebas**, ya que algunas nos permiten cumplir más de un requisito. Por ejemplo, una de las pruebas de persistencia también sirve para demostrar el uso de `@DataJpaTest`. Por eso no vimos necesario crear otra prueba que hiciera exactamente lo mismo.

### Punto 0 — Pruebas del repositorio

En este punto comprobamos que la información se guarde y se consulte correctamente utilizando la base de datos H2.

Primero guardamos una prenda con sus características, variantes y precio. Después consultamos nuevamente esa prenda para verificar que la información siguiera siendo la misma, incluyendo los Value Objects como `Dinero` y `SKU`.

También realizamos una prueba para comprobar el ranking de creadores. Utilizamos datos que conocemos y revisamos que la cantidad de prendas y variantes que muestra la consulta sea correcta y no se cuenten registros repetidos.

**Pruebas utilizadas:**

- [PersistenciaJpaTest](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java): `guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado`.
- [ConsultasAvanzadasJpaTest](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java): `rankingCuentaVariantesYPrendasDistintasCoincideConSqlReal`.

### Punto 1 — Seguridad

Para este punto utilizamos la ruta `GET /api/bolsas/activas/{clienteId}` y realizamos las tres pruebas sobre una misma bolsa registrada en H2.  
En la primera prueba enviamos la solicitud sin token y comprobamos que el sistema respondiera con el código **401**, porque no se había iniciado sesión.  
En la segunda utilizamos un token válido de vendedor y verificamos que respondiera **403**, ya que ese usuario no tiene el rol necesario para acceder a la bolsa.  
En la tercera utilizamos el token del comprador propietario y comprobamos que la respuesta fuera **200**, permitiendo consultar su bolsa correctamente.  
Para estas pruebas utilizamos el inicio de sesión y los tokens reales de la aplicación, sin cambiar las reglas de seguridad que ya estaban configuradas.  
Finalmente, comprobamos que la información de la bolsa no hubiera cambiado después de las solicitudes y que cada respuesta correspondiera al caso evaluado.

**Pruebas utilizadas:**

[BolsaSeguridadSeguimientoIntegrationTest](src/test/java/com/uniquindio/ecommerce/infrastructure/security/BolsaSeguridadSeguimientoIntegrationTest.java)

- `consultarMismaBolsaSinTokenDevuelve401`
- `consultarMismaBolsaConJwtVendedorValidoDevuelve403`
- `consultarMismaBolsaConJwtCompradorPropietarioDevuelve200`

### Punto 2 — @DataJpaTest y @SpringBootTest

En este punto utilizamos `@DataJpaTest` para comprobar que los datos de una prenda se puedan guardar y recuperar correctamente desde la base de datos H2.  
Esta anotación nos permite probar principalmente la parte de persistencia, sin tener que iniciar todos los componentes de la aplicación.  
En nuestra prueba guardamos una prenda, limpiamos el contexto de persistencia y volvimos a consultarla para revisar sus datos, precio, variantes y estado.  
Como estamos utilizando Spring Boot 4, tuvimos en cuenta que `@DataJpaTest` cambió de paquete y que necesita la dependencia `spring-boot-starter-data-jpa-test`, que ya tenemos configurada.  
Por otra parte, `@SpringBootTest` permite cargar la aplicación completa, incluyendo los componentes que necesitamos para realizar pruebas de integración.  
La diferencia que encontramos es que `@DataJpaTest` resulta útil cuando queremos revisar solamente el almacenamiento de información, mientras que `@SpringBootTest` nos sirve para comprobar el funcionamiento de varias partes del sistema.

**Prueba utilizada:**

[PersistenciaJpaTest](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java)

- `guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado`

Esta es la misma prueba utilizada en el punto 0, porque también cumple con el requisito de utilizar `@DataJpaTest`.

### Punto 3 — Prueba de integración

Para este punto utilizamos una prueba que realiza el proceso de compra utilizando los componentes reales que desarrollamos en BOHEME.  
La prueba comienza registrando usuarios e iniciando sesión. Después se publica una prenda y el comprador crea su bolsa para agregar productos.  
Durante el proceso también se comprueba qué sucede cuando se solicita una cantidad superior al stock disponible y luego se continúa con una compra válida.  
En esta prueba participan el controller, los casos de uso, los repositorios y la base de datos H2, sin utilizar simulaciones de estos componentes.  
Al finalizar revisamos las respuestas del sistema y consultamos la base de datos para verificar que el pedido se hubiera guardado y que el stock se actualizara correctamente.  
Consideramos que esta prueba es diferente de una prueba unitaria porque no revisa un solo método de manera aislada, sino el funcionamiento de varias partes del sistema. Es útil cuando necesitamos comprobar procesos completos.

**Prueba utilizada:**

[CompraHttpIntegrationTest](src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraHttpIntegrationTest.java)

- `flujoCompletoConErrorIntencionalStockPedidoYRelecturaDesdeH2`

Aunque una de las pruebas de seguridad también utiliza componentes reales, seleccionamos esta prueba porque permite comprobar un proceso de compra más completo.

### Punto 4 — Más casos en el controller

En este punto realizamos dos pruebas para verificar que el sistema responda correctamente cuando ocurre un problema al agregar productos a una bolsa.

La primera comprueba que, si intentamos agregar un producto a una bolsa que no existe, el sistema devuelva el código **404**.

La segunda comprueba que, si solicitamos una cantidad mayor al stock disponible, el sistema devuelva el código **409**, correspondiente a la regla de negocio `STOCK_INSUFICIENTE`.

En ambos casos utilizamos `verify(..., never())` para comprobar que no se llame al método encargado de guardar los cambios. También verificamos que no se intente guardar información de las prendas cuando la operación es rechazada.

De esta manera comprobamos que el sistema maneje los errores y que no intente guardar una operación que no cumple las condiciones necesarias. Para estas pruebas utilizamos `WithMockUser` para configurar el usuario de prueba, sin evaluar nuevamente la validación de tokens JWT.

**Pruebas utilizadas:**

[CompraControllerSeguimientoTest](src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraControllerSeguimientoTest.java)

- `agregarABolsaInexistenteDevuelve404SinSolicitarGuardado`
- `agregarCantidadSuperiorAlStockDevuelve409SinSolicitarGuardado`

### Resumen de las pruebas del seguimiento

Para presentar los resultados del seguimiento seleccionamos ocho pruebas que permiten comprobar los cinco puntos solicitados.

| Punto | Qué comprobamos | Cantidad de pruebas |
|---|---|---:|
| 0 | Guardar y recuperar información; comprobar el ranking | 2 |
| 1 | Seguridad con respuestas 401, 403 y 200 | 3 |
| 2 | Uso de `@DataJpaTest` y diferencia con `@SpringBootTest` | Se utiliza una prueba del punto 0 |
| 3 | Proceso completo de compra con base de datos real | 1 |
| 4 | Errores 404 y 409, comprobando que no se guarden cambios | 2 |
| **Total** | **Cinco puntos evaluados** | **8 pruebas diferentes** |

Para ejecutar estas pruebas desde IntelliJ IDEA creamos una configuración llamada `Seguimiento - Pruebas avanzadas`, en la que seleccionamos únicamente los métodos correspondientes a esta actividad.

Las demás pruebas del proyecto se mantienen, ya que forman parte de las verificaciones realizadas durante el desarrollo de BOHEME.

### Ejecución de las pruebas

Para ejecutar todas las pruebas del proyecto desde la terminal de Windows utilizamos:

```powershell
.\gradlew.bat test
```

Este comando corresponde a `./gradlew test`, solicitado en las instrucciones de entrega.

En la verificación general realizada anteriormente obtuvimos **224 pruebas exitosas en 23 clases**, sin fallos. De estas, seleccionamos las ocho mencionadas para mostrar de manera específica el cumplimiento del seguimiento.

Como evidencias de la actividad presentamos el enlace al repositorio de GitHub, donde están las pruebas dentro de `src/test/java`, y una captura de la ejecución de Gradle con el resultado exitoso.

La información adicional y los resultados de las pruebas se encuentran en [la documentación del seguimiento](docs/seguimiento-pruebas-avanzadas/README.md).