# BOHEME — Entregable 2, apartado 2.9: flujo completo del negocio

## Resultado verificado

Implementado el recorrido de publicación → selección → pedido persistido → consulta,
con JWT, propietarios, precios históricos, inventario y transacción.

- `.\gradlew.bat clean test`: **219 exitosas**, **0 fallos, 0 errores, 0 omitidas**.
  Se conservan las 189 anteriores y se agregan 30 ejecuciones.
- Colección **Postman ejecutada mediante Newman 6.2.3**: **23 peticiones reales,
  33 aserciones, 0 fallos** contra `http://127.0.0.1:61809`.
- **8 SELECT reales** en la H2 del mismo proceso Spring, repartidos antes y después
  de confirmar. Tras el error: cero pedidos para esa bolsa y stock 3. Tras confirmar:
  un pedido PENDIENTE de dos unidades, total 39,90 COP, bolsa cerrada y stock 1.
- Swagger/OpenAPI conserva las operaciones anteriores y documenta **15 operaciones**.
  El esquema Bearer y los DTO fueron comprobados automáticamente.

No se hicieron commits ni push. No se implementaron frontend, pagos, envíos,
facturación, reembolsos, MariaDB ni actividades ajenas a 2.9.

## Revisión y decisiones de arquitectura

Se revisaron BolsaDeEstilo, LineaDeBolsa, PrendaDeAutor, VariantePrenda, PedidoDeModa,
EstadoPedido, repositorios, casos de uso, controllers, seguridad, mappers,
entidades JPA y datos iniciales antes de modificar el flujo.

Reutilizado:

- `BolsaDeEstilo.agregarLinea`: valida estado, moneda, stock y cantidad acumulada.
- `LineaDeBolsa`: conserva precio elegido desde el catálogo y calcula subtotal.
- `VariantePrenda.reducirStock`: valida cantidad y disponibilidad sin permitir negativos.
- `PrendaDeAutor.actualizarStockVariante`: actualiza la variante dentro del agregado.
- Repositorios/mappers existentes, JWT, UsuarioActual y ErrorResponse.
- Estados y transiciones de PedidoDeModa: la compra nueva comienza PENDIENTE.

Faltaba una línea histórica de pedido con cantidades y dinero, persistencia de
pedidos, consulta por propietario y los dos casos de uso de modificación de bolsa.

Flujo conservado:

`Controller REST → caso de uso transaccional → contratos del dominio → adaptadores JPA → H2`.

El dominio sigue usando únicamente JDK y tipos propios: sin Spring, JPA o HTTP.
Los contratos nuevos usan UUID, IDs y agregados propios. Los bloqueos se implementan
en infraestructura y no introducen LockModeType ni anotaciones JPA en domain.

## Casos de uso y endpoints nuevos

| Caso de uso | Endpoint | Respuesta correcta |
|---|---|---|
| `AgregarVarianteABolsaUseCase` | POST `/api/bolsas/{bolsaId}/items` | 200 con BolsaDeEstiloResponse y subtotales |
| `ConfirmarPedidoUseCase` | POST `/api/bolsas/{bolsaId}/confirmar-pedido` | 201 con PedidoResponse y Location |
| `ConsultarPedidoUseCase` | GET `/api/pedidos/{pedidoId}` | 200 con el histórico del pedido propio |

Los tres requieren COMPRADOR. Los controllers sólo reciben/validan entradas,
invocan casos de uso y convierten a DTO mediante mappers separados.

La solicitud de adición reutiliza el DTO existente, antes sin endpoint, retirando
`clienteId`. Ahora contiene **solamente** `varianteId` y `cantidad` positiva:

```json
{"varianteId":"a7a1ca37-aa4c-4693-802b-bca5d802ec5b","cantidad":2}
```

El comprador no establece precio, moneda, stock ni identidad. El backend obtiene
la prenda real por la variante, verifica oferta/stock y usa su precio persistido.
La identidad procede del JWT validado. Campos JSON extra no deciden esos valores;
una prueba HTTP comprueba expresamente que enviar precio 0,01, USD y stock 999 no
altera el precio real de catálogo, 19,95 COP.

## Validaciones y semántica de cierre

Al agregar:

1. Bloquear y cargar la bolsa; inexistente → 404.
2. Comprobar propietario autenticado; ajeno → 403.
3. Comprobar estado activo; cerrada → 409.
4. Validar cantidad positiva; DTO inválido → 400.
5. Resolver variante y prenda persistidas, excluyendo eliminadas lógicamente.
6. Rechazar prenda suspendida; variante/prenda inexistente → 404.
7. Delegar moneda y stock, incluida cantidad acumulada, al agregado BolsaDeEstilo.
8. Guardar la bolsa. Agregar **no reserva ni descuenta stock**.

Al confirmar se revalidan propietario, estado, contenido, disponibilidad de cada
variante y stock. Bolsa vacía, suspendida, cerrada o selección ya no disponible
produce 409. Un error no genera un pedido ni descuenta parcialmente inventario.

El endpoint anterior PATCH `/api/bolsas/{bolsaId}/cerrar` conserva su significado:
**cerrar sin comprar**; puede cerrar una bolsa vacía y no crea pedidos. Una bolsa
cerrada manualmente ya no puede confirmarse. Confirmar también la cierra, pero
sólo junto al pedido y al descuento, dentro de la misma transacción.

No se permite confirmar dos veces: la segunda llamada devuelve 409 y conserva
el único pedido y el stock de la primera operación.

## Pedido y persistencia

`PedidoDeModa` se amplió con `bolsaId` y líneas históricas. La fábrica
`desdeBolsa(...)` genera el pedido completo en PENDIENTE; `reconstruir(...)` aplica
las mismas invariantes. La fábrica anterior `crear(...)`, su lista de variantes y
las transiciones existentes se conservan para compatibilidad con Entrega 1.
El flujo HTTP utiliza `getLineas()`, no la representación antigua sin cantidades.
El adaptador sólo permite persistir pedidos completos con bolsa y líneas históricas.

`LineaDePedido`, inmutable, conserva:

- UUID de prenda y variante.
- Nombre de la prenda, SKU y talla.
- Cantidad positiva.
- Precio unitario y moneda históricos.

El dominio calcula cada subtotal con BigDecimal (`cantidad × precioUnitario`) y
el total sumando los subtotales de una sola moneda. Se rechazan pedidos vacíos,
datos obligatorios ausentes, variantes repetidas y mezcla de monedas.

Tablas nuevas:

| Tabla | Datos principales |
|---|---|
| `pedidos_moda` | ID, bolsa_id único, cliente_id, fecha_creacion UTC, estado, total, escala del total y moneda; versión JPA |
| `lineas_pedido` | ID técnico, pedido_id, prenda_id, variante_id, nombre, SKU, talla, cantidad, precio unitario, escala, moneda y posición |

Las líneas están relacionadas con su pedido. Referencian el catálogo por UUID,
conservando la información histórica aunque éste cambie o se elimine lógicamente.
Los subtotales se reconstruyen exactamente desde cantidad y precio históricos;
no dependen del precio actual ni requieren una columna redundante.

Se guarda también el total calculado. El mapper lo contrasta con el total de las
líneas al reconstruir: una inconsistencia persistida no se acepta silenciosamente.
La fecha se almacena con precisión `timestamp(9)`; el flujo utiliza LocalDateTime
en UTC. La representación JSON no lleva sufijo Z, por lo que su interpretación UTC
se documenta explícitamente.

No hay endpoint ni contrato de borrado de pedidos. Por eso no se añade una
operación soft delete innecesaria: el histórico de compra se conserva. Los
agregados existentes mantienen su eliminación lógica sin cambios de estrategia.

## Precios, inventario, transacciones y concurrencia

**Política de precio:** se respeta la cotización guardada por el backend al añadir
la variante. Añadir de nuevo la misma variante acumula cantidad conservando el
precio de esa línea. Confirmar revalida oferta e inventario y conserva ese precio;
no acepta precios de la solicitud ni recalcula retroactivamente el histórico.

`ConfirmarPedidoUseCase.ejecutar` tiene una única transacción:

1. Lee la bolsa con `PESSIMISTIC_WRITE`.
2. Comprueba propietario, estado activo y líneas.
3. Resuelve las prendas y las bloquea en orden global de UUID, sin duplicarlas.
4. Revalida cada variante y aplica `reducirStock` en dominio.
5. Construye las líneas históricas y el pedido PENDIENTE.
6. Guarda prendas, cierra/guarda bolsa y registra/flush del pedido.
7. Commit conjunto; cualquier excepción produce rollback.

Los métodos de bloqueo de Spring Data usan `@Lock(PESSIMISTIC_WRITE)` y JPQL
sobre la raíz, sin fetch de colección en el SELECT FOR UPDATE. Después el mapper
carga las líneas/variantes dentro de la transacción. Los bloqueos se mantienen
hasta su finalización. Referencias de soporte:
[Spring Data JPA Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html),
[H2 SELECT y bloqueo](https://h2database.github.io/html/commands.html).

Los casos existentes de stock, suspensión, vaciado y cierre también leen con
este contrato de actualización. Esto evita dejar una escritura pública fuera
del mecanismo que protege la confirmación. Los contratos tienen una implementación
predeterminada compatible con los repositorios en memoria de las pruebas anteriores.

La restricción única `uk_pedido_bolsa` añade una defensa de base de datos frente a
pedidos repetidos. Los conflictos de bloqueo/versión se traducen a 409 con
`CONFLICTO_CONCURRENCIA`; no se convierten en un éxito aparente.

**El inventario de compra es `VariantePrenda.stockDisponible`.** El `stockTotal`
independiente ya existente no se recalcula: cambiar esa regla no corresponde a
este apartado. En la demostración M pasa 3 → 1, L permanece 2 y stockTotal permanece 5.

El pedido PENDIENTE deja esas unidades descontadas/asignadas a la operación, pero
no representa un cobro ni un pedido PAGADO. No se implementó ninguna operación de pago.

## Seguridad y propiedad

Se conserva Resource Server/JWT y la relación `usuario.id = clienteId/creadorId`.
Las dos rutas nuevas de bolsa quedan en la política COMPRADOR existente; GET
`/api/pedidos/{pedidoId}` tiene una política COMPRADOR explícita.

Los nuevos casos de uso comprueban propietario dentro de la operación, usando
UsuarioActual y el propietario recuperado de H2. No confían en un clienteId del JSON.
La consulta del pedido compara su clienteId histórico con el UUID del JWT.

| Situación | HTTP |
|---|---:|
| Sin JWT válido | 401 |
| Vendedor usando operación de comprador / recurso ajeno | 403 |
| Bolsa, variante o pedido inexistente | 404 |
| Cantidad/UUID/JSON inválidos | 400 |
| Stock insuficiente, bolsa cerrada/vacía, oferta suspendida, confirmación duplicada | 409 |

Se reutiliza el manejador global y ErrorResponse; no se exponen trazas ni secretos.

## JSON reales de la demostración

Respuesta real al error intencional de agregar cuatro unidades con stock tres:

```json
{
  "timestamp":"2026-10-25T03:13:47.766566800Z",
  "status":409,
  "code":"STOCK_INSUFICIENTE",
  "message":"No hay suficiente stock en el taller para esta variante de talla.",
  "path":"/api/bolsas/2e02af0f-bb3b-4d44-9042-656233d14829/items",
  "details":{}
}
```

Respuesta real al confirmar dos unidades, HTTP 201:

```json
{
  "pedidoId":"b643920b-69f1-4673-b52d-e2316a8ed658",
  "bolsaId":"2e02af0f-bb3b-4d44-9042-656233d14829",
  "clienteId":"24952836-09e1-45b2-a2c1-7b94a9387a3e",
  "estado":"PENDIENTE",
  "fechaCreacion":"2026-10-25T03:13:51.4507405",
  "lineas":[{
    "prendaId":"4813095b-e9b2-4a8a-9708-44bf973d44ab",
    "varianteId":"a7a1ca37-aa4c-4693-802b-bca5d802ec5b",
    "nombrePrenda":"Camisa colección demo 2.9",
    "sku":"DEMO29-M",
    "tipoTalla":"M",
    "cantidad":2,
    "precioUnitario":{"monto":19.95,"moneda":"COP"},
    "subtotal":{"monto":39.9,"moneda":"COP"}
  }],
  "total":{"monto":39.9,"moneda":"COP"},
  "moneda":"COP"
}
```

La respuesta lleva `Location: /api/pedidos/b643920b-69f1-4673-b52d-e2316a8ed658`.
El GET posterior con el JWT del comprador recuperó ese pedido desde H2.

Los cuerpos reales de las 23 solicitudes/respuestas están en
[error.json](evidencias-2.9/error.json), [final.json](evidencias-2.9/final.json) y
[seguridad.json](evidencias-2.9/seguridad.json). Los JWT completos se omiten de la evidencia publicada.

## Pruebas ejecutadas

| Suite nueva | Ejecuciones | Qué comprueba |
|---|---:|---|
| `PedidoCompraTest` | 2 | Total/subtotales, PENDIENTE, listas inmutables, reconstrucción e invariantes |
| `FlujoCompraIntegrationTest` | 16 | H2 real, adición, reglas, histórico, inventario, duplicado, rollback y concurrencia |
| `CompraHttpIntegrationTest` | 12 | Registro/login/JWT reales, flujo con H2, entradas inválidas, propietarios, 401/403 y OpenAPI |
| Pruebas anteriores | 189 | Conservadas y ejecutadas |
| **Total** | **219** | **0 fallos, 0 errores, 0 omitidas** |

Se utilizó AAA. Las pruebas concurrentes ejecutan hilos, identidades, conexiones
y transacciones distintas sobre H2, con inicio coordinado mediante CountDownLatch:

- Dos compradores piden dos unidades cada uno de un stock tres: un pedido,
  un rechazo StockInsuficienteException y stock uno.
- Dos confirmaciones simultáneas de la misma bolsa: un pedido y un rechazo por bolsa cerrada.
- Dos compras con las mismas prendas añadidas en orden inverso: ambas completan
  sin ciclo de bloqueo y sus stocks quedan correctamente descontados.

Rollback: se provoca una excepción **después de ejecutar el registro y flush reales**
del pedido. Spring traduce la excepción del repositorio; la prueba comprueba su
causa original y verifica cero pedidos/líneas, stock original y bolsa aún abierta.
Otra prueba rechaza una línea sin stock sin descontar parcialmente las demás.

El histórico se comprueba cambiando el precio antes de confirmar y eliminando
lógicamente la prenda después: el pedido mantiene su precio, descripción comercial
y total. Se verifica además que un total SQL corrupto no puede reconstruirse.

Sólo se adaptó una aserción anterior: el conteo OpenAPI pasa de 12 a 15 operaciones.
No se eliminó ni deshabilitó ninguna prueba. No se alteraron fixtures antiguos
para ignorar errores ni se introdujeron verificaciones ajenas a este flujo.

Evidencia: [log de clean test](evidencias-2.9/gradle-clean-test.log),
[resumen JUnit](evidencias-2.9/pruebas.json), carpeta `evidencias-2.9/junit/` y
[HTML de las 219 pruebas](evidencias-2.9/reportes-219-html.zip).
Los reportes anteriores también permanecen en las evidencias de 2.8 y en
`evidencias-2.9/reportes-previos-189-html.zip`.

## Verificación Postman y SQL realizada

La [colección Postman](postman/boheme-2.9.postman_collection.json) y el
[entorno local sin tokens](postman/boheme-local.postman_environment.json) pueden
importarse en Postman. Se ejecutaron de verdad con Newman 6.2.3, el ejecutor de
colecciones de Postman ([documentación oficial](https://learning.postman.com/docs/reference/newman-cli/installing-running-newman)).

| Etapa | HTTP | Aserciones correctas | SELECT de H2 |
|---|---:|---:|---:|
| Preparación y error intencional | 10 | 14 | 4 |
| Compra y confirmación duplicada | 6 | 11 | 4 |
| Seguridad y OpenAPI | 7 | 8 | 0 |
| **Total** | **23** | **33** | **8** |

Los SELECT se ejecutaron mediante la consola local del **mismo proceso Spring**,
con URL `jdbc:h2:mem:ecommercedb;IFEXISTS=TRUE`. No se consultó otra base en memoria.

| Evidencia SQL por bolsa de la demostración | Tras error | Tras compra |
|---|---:|---:|
| Pedidos registrados | 0 | 1 |
| Stock de la variante M | 3 | 1 |
| Bolsa cerrada | FALSE | TRUE |
| Líneas de bolsa tras el error | 0 | — |
| Cantidad en línea de pedido | — | 2 |
| Precio unitario / subtotal / total | — | 19,95 / 39,90 / 39,90 COP |
| Estado | — | PENDIENTE |

Los HTML originales están en `evidencias-2.9/error-*.html` y `final-*.html`.
Las consultas y sus filas también están en los JSON enlazados. Ejemplo de contraste:

```sql
SELECT p.id, p.bolsa_id, p.cliente_id, p.estado, p.fecha_creacion, p.total, p.moneda,
       l.variante_id, l.sku, l.cantidad, l.precio_unitario,
       l.cantidad*l.precio_unitario AS subtotal
FROM pedidos_moda p JOIN lineas_pedido l ON l.pedido_id=p.id
WHERE p.id='b643920b-69f1-4673-b52d-e2316a8ed658';

SELECT stock_disponible FROM variantes_prenda
WHERE id='a7a1ca37-aa4c-4693-802b-bca5d802ec5b';
```

Los logs Postman están en `postman-error.log`, `postman-compra.log` y
`postman-seguridad.log` dentro de la carpeta de evidencias. El error intencional
409 es una respuesta esperada y validada; no un fallo ignorado por el runner.

## Guía para repetir la sustentación en Swagger

Instancia actual: <http://127.0.0.1:61809/swagger-ui/index.html>.
Para un nuevo arranque usar Java 21, configurar BOHEME_JWT_SECRET como Base64 de
al menos 32 bytes aleatorios y activar `dev`. `server.port=0` evita conflictos:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\boheme-temurin-21.0.12.1\jdk-21.0.12.1+1"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
$flujoJwtBytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($flujoJwtBytes)
$env:BOHEME_JWT_SECRET = [Convert]::ToBase64String($flujoJwtBytes)
.\gradlew.bat bootRun '--args=--spring.profiles.active=dev --server.port=0'
```

Consultar el puerto real en `Tomcat started on port ...`. El perfil dev mantiene
la consola H2 sólo local; fuera de dev permanece desactivada y denegada.

1. **Registrar vendedor nuevo**: POST `/api/auth/register`. Usar un correo ficticio
   único, nombre, contraseña `DemostracionBoheme2026!` y rol VENDEDOR. Copiar `id`.
2. **Login vendedor**: POST `/api/auth/login` con ese correo/contraseña. Copiar `token`.
3. **Authorize**: seleccionar bearerAuth y pegar sólo el JWT, sin el prefijo Bearer.
4. **Publicar prenda**: POST `/api/prendas` con el UUID del vendedor como creadorId:

```json
{
  "creadorId":"UUID_DEL_VENDEDOR",
  "nombre":"Camisa colección demo 2.9",
  "descripcion":"Lino de autor para sustentación",
  "precioBase":19.95,
  "moneda":"COP",
  "stockTotal":5,
  "variantes":[
    {"sku":"DEMO29-M","tipoTalla":"M","stockDisponible":3},
    {"sku":"DEMO29-L","tipoTalla":"L","stockDisponible":2}
  ]
}
```

5. Copiar prendaId y varianteId de M; consultar GET `/api/prendas/creador/{creadorId}`.
6. **Registrar comprador nuevo**, con otro correo único y rol COMPRADOR; iniciar
   sesión y reemplazar en Authorize el JWT del vendedor por el del comprador.
7. Crear bolsa con POST `/api/bolsas`, `{"clienteId":"UUID_DEL_COMPRADOR"}`. Copiar bolsaId.
   Usar comprador nuevo permite que la consulta de activa sea inequívoca sin cambiar
   la regla anterior que admite varias bolsas activas por cliente.
8. **Provocar error**: POST `/api/bolsas/{bolsaId}/items` con varianteId y cantidad 4.
   Mostrar 409 STOCK_INSUFICIENTE. Consultar bolsa activa: sin líneas; consultar
   catálogo: M sigue en 3. Mostrar SELECT por bolsa: cero pedidos.
9. Repetir adición con cantidad 2. Respuesta 200 y subtotal 39,90 COP.
10. GET `/api/bolsas/activas/{clienteId}` para mostrar contenido y subtotal.
11. **Confirmar**: POST `/api/bolsas/{bolsaId}/confirmar-pedido`, sin cuerpo.
    No llamar al endpoint de cerrar antes: el cierre manual no es una compra.
12. Mostrar 201, Location, estado PENDIENTE y total. Copiar pedidoId.
13. GET `/api/pedidos/{pedidoId}` con JWT comprador; mostrar líneas históricas.
14. Consultar catálogo: M=1, L=2. Mostrar las filas del pedido y líneas en H2.
15. Opcionalmente repetir confirmación para demostrar 409 sin segundo descuento;
    quitar JWT para obtener 401 o usar vendedor/otro comprador para obtener 403.

**Estado de comprobación visual:** no se afirma haber pulsado Authorize/Execute
en Swagger. Esos clics siguen pendientes de revisión manual; la automatización
de navegador de la sesión anterior no pudo identificar de forma segura la URL
de Chrome. Sí se comprobaron el esquema Bearer, la seguridad por operación, los
DTO, los JWT reales y el flujo completo mediante Postman/Newman y H2.

## Repetición con Postman / Newman

Importar colección y entorno local, seleccionar ese entorno, ajustar baseUrl al
puerto real y ejecutar las tres carpetas en orden. La primera petición genera
un runId único. Los scripts guardan IDs/JWT en el entorno seleccionado y los
utilizan en las siguientes peticiones. Son cuentas ficticias nuevas por ejecución.

Ejecución completa sin pausas SQL:

```powershell
npx.cmd --yes newman@6.2.3 run docs/postman/boheme-2.9.postman_collection.json -e docs/postman/boheme-local.postman_environment.json --bail
```

Para verificar SQL justo después del error, ejecutar por carpetas y exportar el
entorno de ejecución en `.gradle` (ignorado por Git). Ejemplo de la primera etapa:

```powershell
npx.cmd --yes newman@6.2.3 run docs/postman/boheme-2.9.postman_collection.json -e docs/postman/boheme-local.postman_environment.json --folder '01 Preparacion y error' --reporters cli,json --reporter-json-export .gradle/flujo-newman-error.json --export-environment .gradle/flujo-entorno.json --bail
python scripts/evidencia-flujo-2.9.py --phase error --base-url http://127.0.0.1:61809 --variables .gradle/flujo-entorno.json --newman-report .gradle/flujo-newman-error.json
```

Continuar con `02 Compra` usando `-e .gradle/flujo-entorno.json` y exportando ese
mismo entorno; recoger `--phase final`. Después ejecutar `03 Seguridad` y recoger
`--phase seguridad`. Los reportes crudos con JWT temporales quedan en `.gradle`;
el helper publica únicamente datos sanitizados y los SELECT.

Newman y Python son clientes externos de demostración. **El backend no necesita
Node, npm ni Python para funcionar:** las reglas, JWT y transacciones están en Java.
La consulta SQL también puede hacerse manualmente desde la consola H2 del perfil dev.

## Archivos creados y modificados

Inventario contrastado mediante SHA-256 con el estado inicial de esta tarea:
[archivos-fuente.json](evidencias-2.9/archivos-fuente.json). El repositorio ya tenía
cambios sin commit de los apartados anteriores; este inventario no los confunde
con cambios realizados ahora.

Archivos Java **creados**, relativos a `src/main/java/com/uniquindio/ecommerce/`:

| Paquete | Archivos |
|---|---|
| `domain/valueobject` | `LineaDePedido.java` |
| `domain/repository` | `PedidoDeModaRepository.java`, `CatalogoVariantesRepository.java` |
| `application/usecase` | `AgregarVarianteABolsaUseCase.java`, `ConfirmarPedidoUseCase.java`, `ConsultarPedidoUseCase.java` |
| `application/dto/response` | `LineaPedidoResponse.java`, `PedidoResponse.java` |
| `infrastructure/persistence/jpa/entity` | `PedidoJpaEntity.java`, `LineaPedidoJpaEntity.java` |
| `infrastructure/persistence/jpa/repository` | `PedidoJpaRepository.java` |
| `infrastructure/persistence/jpa/mapper` | `PedidoJpaMapper.java` |
| `infrastructure/persistence/jpa` | `JpaPedidoDeModaRepository.java` |
| `infrastructure/rest/mapper` | `PedidoMapper.java` |
| `infrastructure/rest/controller` | `CompraController.java` |

Pruebas creadas, relativas a `src/test/java/com/uniquindio/ecommerce/`:

- `domain/entity/PedidoCompraTest.java`.
- `infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java`.
- `infrastructure/rest/controller/CompraHttpIntegrationTest.java`.

Archivos Java **modificados**, bajo los mismos prefijos:

| Archivo / grupo | Motivo |
|---|---|
| `domain/entity/PedidoDeModa.java` | Líneas históricas, bolsa, total y reconstrucción |
| `domain/repository/BolsaDeEstiloRepository.java`, `PrendaDeAutorRepository.java` | Contratos de lectura para actualización |
| `application/dto/request/AgregarItemBolsaRequest.java` | Retirar clienteId; sólo variante y cantidad |
| `application/usecase/ActualizarStockVariantePrendaUseCase.java`, `SuspenderVentaPrendaDeAutorUseCase.java`, `VaciarBolsaDeEstiloUseCase.java`, `CerrarBolsaDeEstiloUseCase.java` | Participar en el bloqueo de raíz al modificar |
| `infrastructure/persistence/jpa/JpaBolsaDeEstiloRepository.java`, `JpaPrendaDeAutorRepository.java` | Adaptación de lecturas bloqueadas y resolución de variante |
| `infrastructure/persistence/jpa/repository/BolsaJpaRepository.java`, `PrendaJpaRepository.java` | JPQL parametrizada y PESSIMISTIC_WRITE |
| `infrastructure/security/SecurityConfig.java` | GET de pedidos exclusivo COMPRADOR |
| `infrastructure/config/OpenApiConfig.java` | Documentar Bearer de pedidos y rol correcto |
| `infrastructure/rest/exception/ApiExceptionHandler.java` | Respuesta 409 de concurrencia |
| Test `infrastructure/security/SeguridadJwtIntegrationTest.java` | Actualizar únicamente conteo esperado de operaciones 12 → 15 |

Otros archivos creados: este informe, colección y entorno Postman,
`scripts/evidencia-flujo-2.9.py` y los reportes/JSON/HTML SQL de `docs/evidencias-2.9/`.
No fue necesario modificar Gradle, la configuración JWT ni los datos iniciales.

## Tabla de cumplimiento de 2.9

| Requisito | Implementación / comprobación |
|---|---|
| Inicio de sesión y JWT vendedor/comprador | Postman registra/loguea ambos roles; HTTP real y pruebas de integración |
| Publicar y consultar prenda | POST y GET reales antes de comprar |
| Crear bolsa y agregar variante | Nuevos caso de uso/endpoint, propietario y catálogo persistido |
| No confiar en precio/stock/identidad enviados | DTO de dos campos, UsuarioActual, consulta de catálogo; test de campos monetarios extra |
| Stock, cantidad, moneda y bolsa activa | Invariantes reutilizadas y pruebas de error |
| Consultar bolsa/subtotal | GET activa real antes de confirmar |
| Pedido mínimo persistido | PedidoDeModa ampliado, líneas históricas y dos tablas independientes del dominio |
| Cantidad, precio, moneda, subtotal y total | Snapshot y cálculos BigDecimal del dominio; JSON y SQL coincidentes |
| Fecha, comprador y estado | Persistidos y recuperados; estado PENDIENTE, sin pagos |
| Descontar inventario consistentemente | Stock de variante 3 → 1; otra variante intacta |
| No confirmar dos veces | Estado de bolsa, bloqueo y bolsa_id único; POST duplicado y concurrencia verificados |
| Transacción y rollback | Fallo después del flush real deja cero pedidos y stock/bolsa originales |
| Evitar sobreventa concurrente | PESSIMISTIC_WRITE; tres escenarios H2 con hilos/conexiones distintas |
| Histórico al cambiar/eliminar catálogo | Prueba de precio cambiado y soft delete posterior, sin alterar pedido |
| Seguridad/propiedad | 401/403/404/409 en pruebas y comprobaciones HTTP de propietarios |
| Error intencional sin efectos | Postman 409, bolsa vacía, SQL: cero pedidos y stock tres |
| Flujo demostrable en Swagger o Postman | **Ejecutado con Postman/Newman**, 23 HTTP y 33 aserciones correctas |
| Swagger Bearer, DTO y nuevos endpoints | OpenAPI comprobado; clics Authorize/Execute pendientes de revisión manual |
| Conservar 189 y ejecutar clean test | **219/219**, 0 omitidas, sin borrar ni desactivar pruebas |
| DDD y alcance mínimo | Dominio sin JPA/Spring; sin frontend, pagos, MariaDB ni módulos adicionales |

## Limitaciones y pendientes

- La comprobación **visual** de Authorize/Execute en Swagger queda pendiente;
  se proporciona la guía y no se afirma que esos clics se realizaron.
- H2 es volátil: reiniciar pierde compras y recrea datos dev. La colección es
  repetible generando nuevos correos y recursos ficticios en cada corrida.
- No se incorporaron pago, cancelación HTTP ni devolución de inventario; el
  pedido queda PENDIENTE. Tampoco se cambió la regla de stockTotal independiente.
- La representación antigua de PedidoDeModa se conserva por compatibilidad;
  sólo los pedidos nuevos con bolsa y líneas históricas se admiten en persistencia.
- Los avisos de deprecación de bibliotecas/fixtures y JVM no impidieron el build.

El flujo funcional de 2.9 fue implementado y demostrado de punta a punta con
Postman/Newman y SQL real, incluyendo el error de negocio. No hay un fallo
funcional pendiente en ese recorrido; la interacción visual de Swagger está
claramente separada de lo comprobado.
