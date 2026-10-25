# BOHEME API — Entregable 2, apartados 2.4 a 2.9

La API expone 15 operaciones mediante controllers delgados,
DTO validados, mappers manuales y un manejador global de errores.
El dominio no depende de Spring, JPA ni HTTP.

## Operaciones

| Caso de uso | Método | Ruta | Entrada | Salida | Éxito |
|---|---|---|---|---|---|
| RegistrarPrendaDeAutorUseCase | POST | /api/prendas | RegistrarPrendaRequest | PrendaResponse | 201 |
| ListarPrendasPorCreadorUseCase | GET | /api/prendas/creador/{creadorId} | UUID en ruta | PrendaResponse[] | 200 |
| SuspenderVentaPrendaDeAutorUseCase | PATCH | /api/prendas/{prendaId}/suspender | UUID en ruta | Sin cuerpo | 204 |
| ActualizarStockVariantePrendaUseCase | PATCH | /api/prendas/{prendaId}/variantes/{varianteId}/stock | UUID en ruta, ActualizarStockRequest | Sin cuerpo | 204 |
| CrearBolsaDeEstiloUseCase | POST | /api/bolsas | CrearBolsaRequest | BolsaDeEstiloResponse | 201 |
| ObtenerBolsaDeEstiloActivaUseCase | GET | /api/bolsas/activas/{clienteId} | UUID en ruta | BolsaDeEstiloResponse | 200 |
| VaciarBolsaDeEstiloUseCase | DELETE | /api/bolsas/{bolsaId}/items | UUID en ruta | Sin cuerpo | 204 |
| CerrarBolsaDeEstiloUseCase | PATCH | /api/bolsas/{bolsaId}/cerrar | UUID en ruta | Sin cuerpo | 204 |
| RegistrarUsuarioUseCase | POST | /api/auth/register | RegistroRequest | UsuarioResponse | 201 |
| IniciarSesionUseCase | POST | /api/auth/login | LoginRequest | LoginResponse | 200 |
| BuscarPrendasUseCase | GET | /api/prendas | q, creadorId, moneda, page, size | PaginaPrendasResponse | 200 |
| ConsultarVariedadCreadoresUseCase | GET | /api/reportes/creadores/variedad | Sin entrada | VariedadCreadorResponse[] | 200 |
| AgregarVarianteABolsaUseCase | POST | /api/bolsas/{bolsaId}/items | UUID en ruta, AgregarItemBolsaRequest | BolsaDeEstiloResponse | 200 |
| ConfirmarPedidoUseCase | POST | /api/bolsas/{bolsaId}/confirmar-pedido | UUID en ruta, sin cuerpo | PedidoResponse | 201 |
| ConsultarPedidoUseCase | GET | /api/pedidos/{pedidoId} | UUID en ruta | PedidoResponse | 200 |

El listado ahora recupera todas las prendas coincidentes; el contrato anterior
devolvía un único resultado. Una consulta sin coincidencias devuelve `200 []`.
El listado por creador no comprueba la existencia de su cuenta. Al publicar o
modificar una prenda sí se exige rol VENDEDOR y propiedad del recurso.

Registro, login, los GET de prendas y el ranking son públicos. Las escrituras de
prendas requieren VENDEDOR; todas las operaciones de bolsas y la consulta de pedidos
requieren COMPRADOR. Los casos de uso comprueban la propiedad de los recursos.
OpenAPI declara `bearerAuth` de tipo HTTP Bearer, con formato JWT, y documenta
401/403 en cada operación protegida. Login devuelve token, tipo, expiración, rol
y usuario. En Authorize se pega sólo el token, sin el prefijo `Bearer`.

## Ejemplos para Swagger

Registrar una prenda mediante `POST /api/prendas`:

```json
{
  "creadorId": "81000000-0000-4000-8000-000000000001",
  "nombre": "Camisa de lino",
  "descripcion": "Camisa confeccionada a mano en lino",
  "precioBase": 150000.00,
  "moneda": "COP",
  "variantes": [
    {"sku": "CAM-LINO-M", "tipoTalla": "M", "stockDisponible": 5}
  ],
  "stockTotal": 5
}
```

El ejemplo usa el ID de vendedor1 del perfil dev y requiere su JWT. Con otra
cuenta, reemplazar creadorId por usuario.id devuelto por su login.
La respuesta contiene `prendaId` y `variantes[].varianteId`, generados por el
servidor. Usarlos en los parámetros de las operaciones posteriores.

Actualizar una variante mediante PATCH:

```json
{"nuevoStock": 8}
```

Crear una bolsa mediante `POST /api/bolsas`:

```json
{"clienteId": "c1000000-0000-4000-8000-000000000002"}
```

El ejemplo requiere el JWT de comprador2 del perfil dev; con otra cuenta usar
su usuario.id. Usar el `bolsaId` devuelto para agregar artículos, confirmar,
vaciar o cerrar. Las operaciones de suspensión, vaciado, cierre y confirmación
no reciben cuerpo JSON. Las respuestas `204` tampoco lo contienen.

Agregar mediante POST `/api/bolsas/{bolsaId}/items`, reemplazando el UUID por
variantes[].varianteId de la prenda publicada:

```json
{"varianteId": "UUID_DE_LA_VARIANTE", "cantidad": 2}
```

Precio, moneda y stock proceden del catálogo; el comprador no los determina.
Agregar no reserva inventario. Confirmar con POST
`/api/bolsas/{bolsaId}/confirmar-pedido` crea el pedido PENDIENTE, descuenta stock
por variante y cierra la bolsa dentro de una transacción. Devuelve 201, Location
y pedidoId para GET `/api/pedidos/{pedidoId}` con el JWT del mismo comprador.
El pedido conserva cantidades, precios históricos, subtotales y total aunque
cambie el catálogo. No llamar al cierre manual antes de confirmar.

## Validación y errores

- `400 DATOS_INVALIDOS`: restricciones Jakarta, campos anidados, JSON mal formado,
  tipos incorrectos, UUID o tallas inválidos. Se rechazan stocks fraccionarios y
  tallas enviadas como ordinales numéricos; no se convierten silenciosamente.
- `404 RECURSO_NO_ENCONTRADO`: prenda, variante, pedido o bolsa inexistente; bolsa activa
  ausente; ruta desconocida.
- `409 REGLA_DOMINIO`: reglas del dominio, por ejemplo suspender dos veces.
- `409 BOLSA_CERRADA`: agregar, vaciar o confirmar una bolsa cerrada o ya confirmada.
- `409 STOCK_INSUFICIENTE`: cantidad individual o acumulada superior al inventario
  al agregar, o stock insuficiente al confirmar. Actualizar existencias absolutas
  no equivale a reservar o reducir stock.
- `409 CORREO_DUPLICADO`: registro con un correo ya utilizado.
- `409 CONFLICTO_CONCURRENCIA`: conflicto de bloqueo o actualización concurrente.
- `405 METODO_NO_PERMITIDO` y `415 TIPO_CONTENIDO_NO_ADMITIDO`: errores de protocolo.
- `500 ERROR_INTERNO`: mensaje genérico para el cliente; detalles solo en logs.
- `401 NO_AUTENTICADO`: credenciales incorrectas o JWT ausente, inválido o vencido.
- `403 ACCESO_DENEGADO`: rol insuficiente, identidad suplantada o recurso ajeno.

El contrato `ErrorResponse` contiene `timestamp`, `status`, `code`, `message`,
`path` y `details`. `details` identifica los campos inválidos cuando corresponde;
no expone trazas ni detalles internos.

## Límites conservados del modelo

- Prendas, bolsas, usuarios y pedidos se almacenan mediante adaptadores JPA
  en H2. La base sigue siendo en memoria y se pierde al reiniciar. Los adaptadores
  HashMap solo se conservan como dobles de pruebas, sin registro como beans.
  Véase [Persistencia JPA](../persistencia-jpa.md).
- Stock total y stock por variante son campos independientes del modelo actual.
- Los listados incluyen prendas suspendidas. GET /api/prendas tiene filtros y
  paginación, ordena por nombre ASC e ID ASC; el listado por creador no es paginado.
- Puede existir más de una bolsa activa por cliente; el adaptador JPA selecciona
  la primera por ID ascendente, no necesariamente la recién creada.
- Cerrar una bolsa bloquea modificaciones de contenido; no crea pedidos ni pagos.
  Repetir el cierre conserva el estado cerrado.
- El flujo de compra agrega variantes, confirma una sola vez y consulta pedidos
  propios. Revalida oferta y stock al confirmar, con bloqueos de escritura y una
  restricción única de pedido por bolsa. Un error revierte la transacción completa.
- JWT y propiedad se aplican desde 2.7; las cuentas demo sólo se crean en dev.

## Ejecución y documentación

Con Java 21 configurado y BOHEME_JWT_SECRET definido como Base64 de al menos
32 bytes aleatorios (no mostrarlo ni guardarlo en el repositorio):

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootRun '--args=--spring.profiles.active=dev --server.port=0'
```

Consultar el puerto asignado en `Tomcat started on port ...` y reemplazar PUERTO:

- Swagger UI: `http://127.0.0.1:PUERTO/swagger-ui/index.html`
- OpenAPI: `http://127.0.0.1:PUERTO/api-docs`
- Consola H2: `http://127.0.0.1:PUERTO/h2-console` (sólo habilitada en dev).

En dev existen vendedor1@boheme.test y vendedor2@boheme.test, y comprador1@boheme.test
y comprador2@boheme.test. Su contraseña ficticia predeterminada es BohemeDemo2026!,
reemplazable por BOHEME_DEMO_PASSWORD al arrancar. Para una demostración nueva usar
comprador2: inicialmente su bolsa precargada está cerrada; comprador1 ya tiene una
bolsa activa con artículos. Los datos se pierden al reiniciar H2.

Las pruebas MVC usan `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
y `org.springframework.test.context.bean.override.mockito.MockitoBean`, disponibles
mediante las dependencias de pruebas existentes de Spring Boot 4.1.1.
