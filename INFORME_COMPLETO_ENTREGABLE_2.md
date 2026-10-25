# Informe completo del Entregable 2 de BOHEME

Programación Avanzada · Universidad del Quindío · Revisión documental del 25 de octubre de 2026.

**Resultado técnico:** backend de marketplace de ropa de autor con 15 operaciones REST, persistencia JPA/H2, acceso JWT y compra transaccional. Los XML de la última ejecución disponible de Gradle contienen 219 ejecuciones exitosas en 21 clases, sin fallos, errores ni omisiones. Esta tarea documental no volvió a ejecutar ni modificar las pruebas.

El código y las pruebas son la fuente principal; las evidencias históricas describen ejecuciones concretas. La interacción visual Authorize/Execute en Swagger, el video, el README final y la organización final de Git siguen pendientes de comprobación. No se adjudica una calificación ni se inventan integrantes o contribuciones.

Escenario común: vendedor1 publica una prenda de 19,95 COP con talla M de stock 3; comprador2 intenta 4, agrega 2 y confirma por 39,90 COP.

## 1. Introducción

BOHEME conecta compradores con creadores de ropa de autor. El catálogo se organiza alrededor de prendas originales, identidad de su creador, tallas, SKU, precios y disponibilidad. Resuelve la necesidad de publicar, encontrar y convertir la oferta de talleres y marcas independientes en pedidos con cantidades e inventario coherentes. El lenguaje del modelo incluye PrendaDeAutor, BolsaDeEstilo, VariantePrenda y PedidoDeModa.

La Entrega 1 aportó lenguaje de dominio, entidades, objetos de valor, fábricas, reglas y primeros repositorios/casos de uso. Lo respaldan el historial hasta 9230eb1 y los archivos de domain. Ese historial no demuestra modificaciones que aún están sin commit.

La Entrega 2 llevó ese núcleo a una aplicación demostrable: API REST, almacenamiento relacional, consultas avanzadas, autenticación/autorización, pruebas por capas y recorrido de negocio. Se puede registrar y autenticar usuarios, publicar y consultar prendas, actualizar stock y suspender ventas, gestionar bolsas, confirmar una compra y consultar el pedido propio.

No hay frontend, procesamiento de pagos, logística, reembolsos, migración operativa a MariaDB ni administración completa de cuentas. Hay transiciones de pedido antiguas en dominio, pero la API termina en PENDIENTE. Tener un driver MariaDB en Gradle no significa estar conectado a ese motor.

## 2. Arquitectura general

### 2.1 Estructura y dependencias

Java 21, Spring Boot 4.1.1 y Gradle Wrapper 9.7.1 se comprueban en build.gradle y gradle-wrapper.properties. Se usan Web MVC, Validation, Data JPA, Security OAuth2 Resource Server, springdoc 3.0.2, H2 y herramientas de pruebas. Lombok reduce código de infraestructura/aplicación; Hibernate implementa JPA.

DDD sitúa las reglas en objetos que expresan el negocio. domain contiene entity, valueobject, exception, repository, service y query. application contiene usecase, DTO y puertos de seguridad. infrastructure adapta HTTP, Spring Security y JPA. Los casos de uso sí usan @Service y @Transactional: la independencia estricta corresponde al dominio.

~~~mermaid
flowchart LR
  SW[Swagger UI o cliente HTTP] --> SEC[SecurityConfig y JwtDecoder]
  SEC --> C[Controllers REST]
  C --> DTO[DTO validados y mappers REST]
  DTO --> UC[Casos de uso transaccionales]
  C --> AU[AutorizarRecursosUseCase]
  UC --> D[Agregados y Value Objects]
  UC --> P[Puertos de repositorio del dominio]
  P -. implementados por .-> A[Adaptadores Jpa]
  A --> M[Mappers de persistencia]
  M --> D
  A --> SD[Spring Data JPA e Hibernate]
  SD --> H[(H2 en la JVM)]
  UC --> SP[UsuarioActual CodificadorContrasenas EmisorTokens]
  SP -. adaptados por .-> JWT[SecurityContext BCrypt y Nimbus]
~~~

### 2.2 Responsabilidades y beneficios

Una entidad conserva identidad: una variante puede reemplazarse por otra instancia con el mismo UUID y nuevo stock. Un Value Object protege valores, como Dinero frente a un double sin moneda. La raíz controla componentes y operaciones de negocio para preservar consistencia.

Los casos de uso coordinan carga, autorización cuando corresponde, dominio y guardado. Los DTO definen el contrato JSON y validan formato. Los mappers REST convierten DTO/dominio; los JPA reconstruyen dominio/filas. Así no se exponen entidades gestionadas por Hibernate ni se trasladan decisiones HTTP al modelo.

Los puertos de repositorio expresan necesidades con tipos propios. Los adaptadores JPA deciden consultas, bloqueos y almacenamiento. H2 reside en la JVM; JWT vincula identidad autenticada y actores del negocio.

**Límites reales:** LineaDeBolsa se denomina Value Object, pero equals/hashCode comparan varianteId, no todos los atributos. Usuario no redefine equals/hashCode. IniciarSesionUseCase y EmisorTokens devuelven LoginResponse; no es exacto afirmar que ningún caso de uso conoce DTO. Son compromisos del código actual.

Fuentes: [build.gradle](build.gradle), [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties), [src/main/java/com/uniquindio/ecommerce/infrastructure/config/DomainServiceConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/config/DomainServiceConfig.java), [src/main/java/com/uniquindio/ecommerce/infrastructure/config/JsonConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/config/JsonConfig.java) · strictApiNumbers() · línea 12.

## 3. Correcciones de la Entrega 1

Las observaciones de la profesora suministradas para esta revisión son constructores privados y validaciones/invariantes. No se encontró una devolución docente formal que permita atribuir otras observaciones.

**Los constructores de las siete entidades originales ya eran privados.** Se verificó y conservó la visibilidad y las fábricas crear; se añadió una prueba por reflexión. No se afirma que se privatizaran todos. El diff frente a HEAD permite mostrar correcciones locales de reglas y encapsulación, pero no un commit nuevo de esas correcciones.

Se reforzaron identificadores obligatorios, listas nulas/con elementos nulos, stock, monedas, acumulaciones sin desbordamiento y protección de colecciones. En Creador, List.copyOf en la lista interna impedía agregar: ahora se usa ArrayList internamente y una copia inmutable al salir; se protege la última vitrina. En VitrinaDeCreador se copia el catálogo y se corrige la comparación de UUID al eliminar.

| Observación | Problema encontrado | Corrección aplicada | Archivo | Método | Evidencia de prueba |
| --- | --- | --- | --- | --- | --- |
| Constructores privados | Evitar atribuir una modificación inexistente | Verificar y conservar visibilidad y fábricas | [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) | PrendaDeAutor | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · entidades_SoloExponenConstructoresPrivadosYFabricaCrear() · línea 54 |
| Invariantes | Entradas y mutaciones requieren validaciones uniformes | ID, nombre, precio positivo, stock, listas; sin setter libre | [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) | actualizarStock | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · prenda_ActualizaStockValidoYRechazaNegativoSinMutar() · línea 108 |
| Invariantes | Actualizar variante conservando identidad | Reemplazo inmutable a través de la raíz | [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) | actualizarStockVariante | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · prenda_ActualizaVarianteCorrectaConservandoIdentidadYOriginalInmutable() · línea 138 |
| Invariantes | Cantidad acumulada y monedas inconsistentes | Validar moneda, estado y suma en long antes de mutar | [src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java) | agregarLinea | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · bolsa_CantidadAcumuladaNoDesbordaEntero() · línea 278 |
| Invariantes | Lista interna inmodificable y contenedor expuesto | Copia mutable interna y salida inmutable; conservar una vitrina | [src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java) | eliminarVitrina | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · creador_EncapsulaVitrinasYConservaAlMenosUnaAlEliminar() · línea 328 |
| Invariantes | Catálogo compartido y comparación wrapper/UUID | Copiar lista, rechazar nulos y comparar getId().value() | [src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java) | eliminarPrenda | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · vitrina_EncapsulaCatalogoYPermiteOperacionesValidas() · línea 350 |
| Invariantes | Entradas y stock inválidos | Excepción de dominio y estado intacto ante rechazo | [src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java) | reducirStock | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · varianteDeTalla_ValidaCamposYConservaStockAnteRechazo() · línea 215 |
| Invariantes | Listas y saltos de estado del pedido | Validación y secuencia explícita | [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) | despachar | [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) · pedido_RespetaSecuenciaYRechazaSaltosSinCambiarEstado() · línea 405 |

La prueba de constructores cubre las siete entidades originales, no Usuario, incorporado después. Las otras 20 pruebas de InvariantesEntidadesTest ejercitan comportamiento y preservación del estado ante errores. Usuario tiene constructor privado comprobable en código.

Para antes/después usar git diff -- src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java, y repetir con PrendaDeAutor, BolsaDeEstilo y VitrinaDeCreador. Señalar que son cambios locales. El diseño mejora al canalizar mutaciones por operaciones validadas y evitar cambios silenciosos desde listas externas; no se promete inmutabilidad profunda de todas las entidades antiguas.

## 4. Implementación del dominio

### 4.1 Entidades y límites de agregados

PrendaDeAutor es raíz de catálogo: UUID, creadorId, nombre, precio, variantes y disponibilidad. VariantePrenda tiene identidad y stock final; reducirStock devuelve una nueva instancia, que la raíz reemplaza conservando ID. Precio y suspensión devuelven una raíz nueva con la misma identidad. StockTotal es independiente del inventario por talla.

BolsaDeEstilo es raíz de selección: ID/clienteId finales, líneas encapsuladas y estado abierta/cerrada. agregarLinea valida stock individual/acumulado, moneda y estado; vaciar sólo actúa en activa. cerrarBolsa es idempotente y no compra. reconstruir conserva el histórico sin simular inventario actual.

PedidoDeModa es raíz de compra. desdeBolsa exige bolsa, comprador, fecha y líneas válidas, sin variantes repetidas y de una moneda. calcularTotal suma BigDecimal. El constructor antiguo crear con List<VariantePrenda> se conserva; PedidoJpaMapper no permite persistirlo sin bolsa y líneas históricas.

Usuario es la cuenta de acceso, separada de Creador. Exige nombre, correo normalizado, rol y hash con formato BCrypt. Creador y VitrinaDeCreador son del modelo inicial, sin repositorios JPA ni endpoints propios. Vitrina contiene objetos PrendaDeAutor y no sólo IDs: no debe presentarse como un límite de agregado perfectamente aislado por IDs. La API usa creadorId sin cargar ese perfil.

VarianteDeTalla es la representación antigua de talla, color, precio y stock identificada por SKU, fuera del recorrido REST. Su reducirStock devuelve false sin stock. La compra emplea VariantePrenda y StockInsuficienteException.

| Entidad | Responsabilidad | Operaciones verificadas |
| --- | --- | --- |
| PrendaDeAutor | Raíz de catálogo | [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) · crear() · línea 63; [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) · reconstruir() · línea 68; [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) · actualizarStockVariante() · línea 107; [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) · suspenderVenta() · línea 122 |
| VariantePrenda | Componente con identidad y reemplazo | [src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java) · crear() · línea 38; [src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java) · reducirStock() · línea 42; [src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java) · actualizarStock() · línea 52 |
| BolsaDeEstilo | Raíz de selección | [src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java) · crear() · línea 36; [src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java) · agregarLinea() · línea 61; [src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java) · vaciar() · línea 110; [src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java) · cerrarBolsa() · línea 90; [src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java) · reconstruir() · línea 44 |
| PedidoDeModa | Raíz de pedido; compra REST PENDIENTE | [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · crear() · línea 78; [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · desdeBolsa() · línea 59; [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · reconstruir() · línea 64; [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · calcularTotal() · línea 69; [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · autorizarPago() · línea 97; [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · despachar() · línea 104; [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · completar() · línea 111; [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) · cancelar() · línea 118 |
| Usuario | Cuenta de acceso | [src/main/java/com/uniquindio/ecommerce/domain/entity/Usuario.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Usuario.java) · reconstruir() · línea 30; [src/main/java/com/uniquindio/ecommerce/domain/entity/Usuario.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Usuario.java) · normalizarCorreo() · línea 22 |
| Creador | Perfil sin endpoint | [src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java) · crear() · línea 39; [src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java) · agregarVitrina() · línea 67; [src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java) · eliminarVitrina() · línea 74 |
| VitrinaDeCreador | Catálogo antiguo | [src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java) · crear() · línea 41; [src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java) · agregarPrenda() · línea 52; [src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java) · eliminarPrenda() · línea 59 |
| VarianteDeTalla | Representación antigua | [src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java) · crear() · línea 32; [src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java) · reducirStock() · línea 57 |

### 4.2 Value Objects y lecturas

Dinero combina BigDecimal y moneda: admite cero, rechaza negativo y moneda vacía. Prenda y LineaDePedido añaden precio estrictamente positivo. La igualdad de record conserva la escala BigDecimal; persistencia guarda la escala para restaurarla. No se usa double para dinero.

SKU rechaza nulo/blanco y normaliza espacios y mayúsculas. PrendaDeAutorId, BolsaDeEstiloId y VariantePrendaId reducen confusión entre identidades. MedidasCorporales exige medidas positivas y no aparece en la API. TipoTalla enumera XS, S, M, L, XL, XXL; Rol tiene COMPRADOR/VENDEDOR. EstadoPedido incluye más estados que la API de esta entrega.

LineaDeBolsa exige variante, cantidad positiva y precio; actualizarCantidad la reemplaza y calcularSubtotal multiplica. LineaDePedido es una instantánea inmutable con prenda, variante, nombre, SKU, talla, cantidad y precio. ConsultaPrendas valida page >= 0, size 1 a 100 y desplazamiento sin overflow; Pagina y VariedadCreador evitan exponer Page/Pageable al dominio.

| Tipo | Archivo |
| --- | --- |
| BolsaDeEstiloId | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/BolsaDeEstiloId.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/BolsaDeEstiloId.java) |
| Dinero | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/Dinero.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/Dinero.java) |
| EstadoPedido | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/EstadoPedido.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/EstadoPedido.java) |
| LineaDeBolsa | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDeBolsa.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDeBolsa.java) |
| LineaDePedido | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDePedido.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDePedido.java) |
| MedidasCorporales | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/MedidasCorporales.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/MedidasCorporales.java) |
| PrendaDeAutorId | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/PrendaDeAutorId.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/PrendaDeAutorId.java) |
| Rol | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/Rol.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/Rol.java) |
| SKU | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/SKU.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/SKU.java) |
| TipoTalla | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/TipoTalla.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/TipoTalla.java) |
| VariantePrendaId | [src/main/java/com/uniquindio/ecommerce/domain/valueobject/VariantePrendaId.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/VariantePrendaId.java) |

### 4.3 Excepciones y servicios

ReglaDominioException deriva de RuntimeException. StockInsuficienteException y ModificacionBolsaNoPermitidaException permiten errores 409 específicos. RecursoNoEncontradoException y BolsaDeEstiloNoEncontradaException permiten 404. CorreoDuplicadoException distingue un registro repetido. Algunos objetos de valor usan IllegalArgumentException y PrendaDeAutorId usa Objects.requireNonNull: no todas las validaciones arrojan la misma clase.

ActualizadorDeStock llama actualizarStock y guardarPrenda; GestorDeBolsaDeEstilo consulta por ID y filtra estado activo. DomainServiceConfig los crea sin anotaciones Spring en dominio. La API de stock por variante usa un caso de uso distinto al servicio de stock total.

## 5. Casos de uso

Existen 16 clases en application/usecase: ocho originales, dos consultas, tres de autenticación/autorización y tres del flujo de compra. Quince operaciones HTTP no implican quince clases: AutorizarRecursosUseCase es transversal. La autorización antigua se invoca desde los controllers; los tres casos de compra comprueban identidad propia. No afirmar que toda llamada directa a cualquier caso de uso esté protegida por rol.

| Caso y archivo/método | Actor | Entrada y resultado | Puertos utilizados | Reglas y coordinación | Endpoint |
| --- | --- | --- | --- | --- | --- |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarUsuarioUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarUsuarioUseCase.java) · ejecutar() · línea 18 | Público | nombre, correo, contraseña, Rol → Usuario | UsuarioRepository; CodificadorContrasenas | Normalizar correo, mínimo 12 caracteres/máximo 72 bytes, correo único y BCrypt | POST /api/auth/register |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/IniciarSesionUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/IniciarSesionUseCase.java) · ejecutar() · línea 16 | Público | correo, contraseña → LoginResponse | UsuarioRepository; CodificadorContrasenas; EmisorTokens | Normalizar, verificar hash y cuenta activa; rechazo uniforme | POST /api/auth/login |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarPrendaDeAutorUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarPrendaDeAutorUseCase.java) · ejecutar() · línea 17 | Vendedor | PrendaDeAutor ya creada por mapper → raíz guardada | PrendaDeAutorRepository | crear valida la prenda antes del caso; guardarPrenda persiste | POST /api/prendas |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ListarPrendasPorCreadorUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ListarPrendasPorCreadorUseCase.java) · ejecutar() · línea 20 | Público | UUID creador → List<PrendaDeAutor> | PrendaDeAutorRepository | ID no nulo; listado no certifica existencia de cuenta | GET /api/prendas/creador/{creadorId} |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/SuspenderVentaPrendaDeAutorUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/SuspenderVentaPrendaDeAutorUseCase.java) · ejecutar() · línea 20 | Vendedor propio | PrendaDeAutorId → sin retorno | PrendaDeAutorRepository | Lectura bloqueada, suspenderVenta y guardar; repetir suspensión es regla inválida | PATCH /api/prendas/{prendaId}/suspender |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ActualizarStockVariantePrendaUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ActualizarStockVariantePrendaUseCase.java) · ejecutar() · línea 21 | Vendedor propio | PrendaDeAutorId, VariantePrendaId, int → sin retorno | PrendaDeAutorRepository | Lectura bloqueada; actualizarStockVariante valida pertenencia y stock no negativo | PATCH /api/prendas/{prendaId}/variantes/{varianteId}/stock |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/CrearBolsaDeEstiloUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/CrearBolsaDeEstiloUseCase.java) · ejecutar() · línea 20 | Comprador propio | UUID cliente → BolsaDeEstilo | BolsaDeEstiloRepository | BolsaDeEstilo.crear con UUID aleatorio; admite varias activas | POST /api/bolsas |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ObtenerBolsaDeEstiloActivaUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ObtenerBolsaDeEstiloActivaUseCase.java) · ejecutar() · línea 20 | Comprador propio | UUID cliente → Optional<BolsaDeEstilo> | BolsaDeEstiloRepository | ID no nulo; controller transforma ausencia en 404 | GET /api/bolsas/activas/{clienteId} |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/VaciarBolsaDeEstiloUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/VaciarBolsaDeEstiloUseCase.java) · ejecutar() · línea 20 | Comprador propio | BolsaDeEstiloId → sin retorno | BolsaDeEstiloRepository | Lectura bloqueada, vaciar sólo activa y guardar | DELETE /api/bolsas/{bolsaId}/items |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/CerrarBolsaDeEstiloUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/CerrarBolsaDeEstiloUseCase.java) · ejecutar() · línea 20 | Comprador propio | BolsaDeEstiloId → sin retorno | BolsaDeEstiloRepository | Lectura bloqueada, cerrarBolsa y guardar; no crea pedido | PATCH /api/bolsas/{bolsaId}/cerrar |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/BuscarPrendasUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/BuscarPrendasUseCase.java) · ejecutar() · línea 16 | Público | ConsultaPrendas → Pagina<PrendaDeAutor> | ConsultaPrendasRepository | Criterios validados; delega búsqueda a la base | GET /api/prendas |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarVariedadCreadoresUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarVariedadCreadoresUseCase.java) · ejecutar() · línea 16 | Público | Sin entrada → List<VariedadCreador> | ConsultaPrendasRepository | Delega ranking; no inventa ventas | GET /api/reportes/creadores/variedad |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/AgregarVarianteABolsaUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/AgregarVarianteABolsaUseCase.java) · ejecutar() · línea 19 | Comprador propio | UUID bolsa, UUID variante, cantidad → BolsaDeEstilo | BolsaDeEstiloRepository; PrendaDeAutorRepository; CatalogoVariantesRepository; UsuarioActual | Bolsa y prenda bloqueadas, propietario, activa, oferta vigente, cantidad y agregarLinea | POST /api/bolsas/{bolsaId}/items |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ConfirmarPedidoUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ConfirmarPedidoUseCase.java) · ejecutar() · línea 20 | Comprador propio | UUID bolsa → PedidoDeModa | BolsaDeEstiloRepository; PrendaDeAutorRepository; CatalogoVariantesRepository; PedidoDeModaRepository; UsuarioActual | Revalidar, reducirStock, snapshot, guardar inventario, cerrar bolsa y registrar en transacción | POST /api/bolsas/{bolsaId}/confirmar-pedido |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarPedidoUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarPedidoUseCase.java) · ejecutar() · línea 15 | Comprador propio | UUID pedido → PedidoDeModa | PedidoDeModaRepository; UsuarioActual | 404 si no existe y 403 si clienteId ajeno | GET /api/pedidos/{pedidoId} |

Autorización transversal: [src/main/java/com/uniquindio/ecommerce/application/usecase/AutorizarRecursosUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/AutorizarRecursosUseCase.java) · identidad() · línea 18; prenda() consulta creadorId y bolsa() consulta clienteId. Usa UsuarioActual, PrendaDeAutorRepository y BolsaDeEstiloRepository. Se relaciona con crear bolsa/prenda, actualizar/suspender prenda y consultar/vaciar/cerrar bolsa. La identidad no se concede por enviar un UUID en JSON.

## 6. Apartado 2.4 API REST

Los cinco controllers actuales son AuthController, PrendaController, BolsaDeEstiloController, ReporteController y CompraController. Reciben parámetros HTTP, validan DTO con @Valid, delegan autorización/coordinación y convierten resultados. No calculan precios, subtotales ni descuentos de inventario.

La validación Jakarta incluye @NotNull, @NotBlank, @Size, @PositiveOrZero, @DecimalMin, @Email, @Pattern y @Min. Las variantes se validan en cascada. Registro exige al menos 12 caracteres y hasta 72 bytes UTF-8; login valida tamaño y formato. JsonConfig impide truncar fracciones a enteros y usar números como ordinales de enum. El patrón de moneda comprueba tres letras, no la existencia en un catálogo ISO.

### 6.1 Las quince operaciones actuales

| Método | Ruta | Acceso | Entrada | Salida | Éxito |
| --- | --- | --- | --- | --- | --- |
| POST | /api/auth/register | Público | RegistroRequest | UsuarioResponse | 201 |
| POST | /api/auth/login | Público | LoginRequest | LoginResponse | 200 |
| POST | /api/prendas | VENDEDOR propio | RegistrarPrendaRequest | PrendaResponse | 201 |
| GET | /api/prendas/creador/{creadorId} | Público | UUID | PrendaResponse[] | 200 |
| PATCH | /api/prendas/{prendaId}/suspender | VENDEDOR propio | UUID, sin cuerpo | Sin cuerpo | 204 |
| PATCH | /api/prendas/{prendaId}/variantes/{varianteId}/stock | VENDEDOR propio | ActualizarStockRequest | Sin cuerpo | 204 |
| POST | /api/bolsas | COMPRADOR propio | CrearBolsaRequest | BolsaDeEstiloResponse | 201 |
| GET | /api/bolsas/activas/{clienteId} | COMPRADOR propio | UUID | BolsaDeEstiloResponse | 200 |
| DELETE | /api/bolsas/{bolsaId}/items | COMPRADOR propio | UUID, sin cuerpo | Sin cuerpo | 204 |
| PATCH | /api/bolsas/{bolsaId}/cerrar | COMPRADOR propio | UUID, sin cuerpo | Sin cuerpo | 204 |
| GET | /api/prendas | Público | q, creadorId, moneda, page, size | PaginaPrendasResponse | 200 |
| GET | /api/reportes/creadores/variedad | Público | Sin entrada | VariedadCreadorResponse[] | 200 |
| POST | /api/bolsas/{bolsaId}/items | COMPRADOR propio | AgregarItemBolsaRequest | BolsaDeEstiloResponse | 200 |
| POST | /api/bolsas/{bolsaId}/confirmar-pedido | COMPRADOR propio | UUID, sin cuerpo | PedidoResponse | 201 |
| GET | /api/pedidos/{pedidoId} | COMPRADOR propio | UUID | PedidoResponse | 200 |

### 6.2 Mappers y contratos JSON

PrendaMapper.toDomain crea UUID de prenda/variantes y llama fábricas; toResponse expone precio y variantes. BolsaDeEstiloMapper arma líneas/subtotales del dominio. ConsultaPrendasMapper conserva content, totalElements, totalPages, page y size y mapea el ranking. PedidoMapper expone el histórico con pedidoId, bolsaId, clienteId, estado, fechaCreacion, lineas, total y moneda. UsuarioMapper excluye contraseña y hash. DineroMapper conserva monto y moneda.

AgregarItemBolsaRequest sólo contiene varianteId y cantidad: no recibe un precio confiable del comprador. CrearBolsaRequest sí recibe clienteId y el caso de autorización exige que coincida con JWT. Los ejemplos UUID genéricos de Swagger deben sustituirse por usuario.id, aunque tengan formato correcto.

Ejemplo de entrada de selección (reemplazar por varianteId obtenido al publicar):

~~~json
{"varianteId":"UUID_COPIADO_DE_VARIANTES","cantidad":2}
~~~

La respuesta de bolsa incluye bolsaId, clienteId, lineas y cerrada. Cada línea incluye precioUnitario y subtotal; la bolsa no tiene un campo total global. PedidoResponse sí tiene total. Confirmar devuelve además Location. Un 204 válido no incluye JSON.

### 6.3 Errores y OpenAPI

ApiExceptionHandler produce timestamp, status, code, message, path y details. Mapea validación a 400 DATOS_INVALIDOS; ausencia/token inválido a 401 NO_AUTENTICADO; rol/propiedad a 403 ACCESO_DENEGADO; recurso ausente a 404 RECURSO_NO_ENCONTRADO; reglas a 409 REGLA_DOMINIO, BOLSA_CERRADA o STOCK_INSUFICIENTE. Hay 409 CORREO_DUPLICADO y CONFLICTO_CONCURRENCIA. Protocolo: 405, 406, 415. Un inesperado produce 500 genérico y registra detalles sólo en logs.

OpenApiConfig declara esquema HTTP bearerAuth/JWT y añade 401/403, seguridad y rol a cada operación protegida. Swagger UI se sirve en /swagger-ui/index.html y el esquema en /api-docs. La existencia del documento no demuestra haber pulsado Authorize/Execute. Esa interacción visual continúa pendiente.

Traza HTTP: [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/exception/ApiExceptionHandler.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/exception/ApiExceptionHandler.java) · businessRule() · línea 67; [src/main/java/com/uniquindio/ecommerce/infrastructure/config/OpenApiConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/config/OpenApiConfig.java) · seguridadOperaciones() · línea 24; [src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraHttpIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraHttpIntegrationTest.java) · swaggerDocumentaTresOperacionesConBearerYDtoSinPrecioEnSolicitud() · línea 130.

## 7. Apartado 2.5 Persistencia con H2 y JPA

### 7.1 Configuración y modelo relacional

application.properties configura jdbc:h2:mem:ecommercedb, usuario sa, contraseña vacía, ddl-auto=create-drop y open-in-view=false. Sin dev, consola y seed están apagados. application-dev.properties activa consola local, datos y cuentas ficticias y enlaza a 127.0.0.1. El secreto JWT se recibe del entorno sin valor predeterminado. La base vive en la misma JVM: abrir otra conexión desde otro proceso Java con el mismo nombre no consulta la misma base.

Las entidades de dominio no tienen @Entity, @Column ni @OneToMany. Las JPA tienen constructores sin argumentos protegidos para Hibernate. Se distinguen identidad del negocio y UUID técnico de líneas. No existe tabla creador ni vitrina. Hay siete tablas actuales; el informe histórico 2.5 sólo describía las cuatro existentes en aquella etapa.

| Tabla | Entidad JPA | Columnas relevantes |
| --- | --- | --- |
| prendas_autor | [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PrendaJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PrendaJpaEntity.java) | id, creador_id, nombre, descripcion, precio_base, precio_escala, moneda, stock_total, disponible_para_venta, version, eliminado |
| variantes_prenda | [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/VarianteJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/VarianteJpaEntity.java) | id, prenda_id, sku, tipo_talla, stock_disponible, posicion, eliminado |
| bolsas_estilo | [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/BolsaJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/BolsaJpaEntity.java) | id, cliente_id, cerrada, version, eliminado |
| lineas_bolsa | [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaBolsaJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaBolsaJpaEntity.java) | id, bolsa_id, variante_id, cantidad, precio_unitario, precio_escala, moneda, posicion, eliminado |
| usuarios | [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/UsuarioJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/UsuarioJpaEntity.java) | id, nombre, correo, password_hash, rol, activo |
| pedidos_moda | [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PedidoJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PedidoJpaEntity.java) | id, bolsa_id único, cliente_id, fecha_creacion, estado, total, total_escala, moneda, version |
| lineas_pedido | [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaPedidoJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaPedidoJpaEntity.java) | id, pedido_id, prenda_id, variante_id, nombre_prenda, sku, tipo_talla, cantidad, precio_unitario, precio_escala, moneda, posicion |

~~~mermaid
erDiagram
  prendas_autor ||--|{ variantes_prenda : "FK prenda_id"
  bolsas_estilo ||--o{ lineas_bolsa : "FK bolsa_id"
  pedidos_moda ||--|{ lineas_pedido : "FK pedido_id"
  usuarios { uuid id PK }
  prendas_autor { uuid id PK
    uuid creador_id }
  variantes_prenda { uuid id PK
    uuid prenda_id FK }
  bolsas_estilo { uuid id PK
    uuid cliente_id }
  lineas_bolsa { uuid id PK
    uuid bolsa_id FK
    uuid variante_id }
  pedidos_moda { uuid id PK
    uuid bolsa_id UK
    uuid cliente_id }
  lineas_pedido { uuid id PK
    uuid pedido_id FK
    uuid prenda_id
    uuid variante_id }
~~~

El diagrama muestra sólo las FK ORM reales. creador_id/cliente_id, pedidos_moda.bolsa_id y referencias de catálogo del histórico son UUID escalares, no relaciones JPA hacia usuarios, bolsa o catálogo. La unicidad de bolsa_id impide dos pedidos por bolsa, pero no es una FK. DineroJpa es @Embeddable y no crea tabla.

### 7.2 Adaptadores y reconstrucción

JpaPrendaDeAutorRepository implementa escritura de prendas, ConsultaPrendasRepository y CatalogoVariantesRepository. JpaBolsaDeEstiloRepository implementa bolsas; JpaUsuarioRepository, usuarios; JpaPedidoDeModaRepository, pedidos. Los InMemory son HashMap sin registro como beans, utilizados explícitamente en pruebas.

PrendaJpaMapper y BolsaJpaMapper sincronizan por ID las filas gestionadas y sus colecciones; conservan identidad y orden. DineroJpaMapper conserva BigDecimal y escala: H2 usa DECFLOAT(100000), decisión que requiere revisión al migrar de motor. PedidoJpaMapper guarda snapshots y comprueba al reconstruir que el total persistido coincide con el dominio. UsuarioJpaMapper reconstruye una cuenta con su hash.

Las lecturas mapean dentro de transacciones; EntityGraph y fetch cubren las colecciones con open-in-view=false. Las raíces Prenda/Bolsa/Pedido usan @Version. No se promete que un snapshot de dominio aislado conserve una versión para cualquier patrón futuro de escritura. La compra se protege con bloqueos explícitos adicionales.

### 7.3 Soft delete

PrendaJpaEntity, VarianteJpaEntity, BolsaJpaEntity y LineaBolsaJpaEntity usan @SoftDelete sobre eliminado. Borrar pasa a UPDATE y Hibernate filtra activos en ORM. Las colecciones de prendas/bolsas usan cascada y orphanRemoval. Pedido y sus líneas no tienen soft delete; sus líneas se guardan con CascadeType.PERSIST y no orphanRemoval. Usuario tampoco tiene soft delete.

Vaciar bolsa marca sus líneas como eliminadas, conservando la raíz. Suspender venta cambia disponible_para_venta y cerrar bolsa cambia cerrada: no equivalen a eliminación lógica. No existe endpoint para eliminar raíces. Los métodos eliminarLogicamente de adaptadores son capacidades de infraestructura verificadas en pruebas; no deben inventarse rutas DELETE de prenda.

### 7.4 Seed y límites

En dev hay tres prendas, cinco variantes, dos bolsas y tres líneas; comprador1 tiene bolsa activa con artículos y comprador2 una cerrada. UsuariosDemo añade cuatro cuentas. DatosIniciales.run usa fábricas y repositorios con IDs estables y conteos que incluyen tombstones, evitando duplicar o resucitar eliminados.

H2 se pierde al reiniciar y create-drop retira el esquema al cerrar. La persistencia comprobada significa escritura y relectura en esa ejecución, no durabilidad tras apagar. Usuario y pedido se añadieron después del informe 2.5. El dominio no debe depender de JPA para que las reglas y pruebas sigan válidas al sustituir almacenamiento.

Pruebas clave: [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java) · guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado() · línea 55; [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java) · vaciarYAgregarDeNuevo_ConservaHistorialDeLineasSinDuplicadosActivos() · línea 180; [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/CasosDeUsoJpaIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/CasosDeUsoJpaIntegrationTest.java) · aplicacion_UtilizaJpaYCargaDatosIdempotentesSinResucitarEliminados() · línea 50.

## 8. Apartado 2.6 Consultas avanzadas

GET /api/prendas acepta q, creadorId, moneda, page (0) y size (20, entre 1 y 100). Los filtros se combinan con AND; q busca en nombre o descripción sin distinguir mayúsculas. Los caracteres %, _ y ! se escapan y se buscan literalmente. q/moneda en blanco omiten criterio. La moneda se normaliza a mayúsculas. Se ordena por nombre ASC e ID ASC, se incluyen suspendidas y se excluyen eliminadas.

ConsultaPrendas comprueba límites y overflow de desplazamiento. El controller sólo construye criterios; BuscarPrendasUseCase delega. JpaPrendaDeAutorRepository prepara valores y PageRequest; no descarga el catálogo para filtrar en Java.

### 8.1 JPQL obtenida de PrendaJpaRepository

Estas consultas se citan desde el código de producción; las entidades y atributos son JPA, no nombres SQL.

Consulta 1:

~~~jpql
select p.id from PrendaJpaEntity p
            where (:texto is null or lower(p.nombre) like :texto escape '!'
                or lower(p.descripcion) like :texto escape '!')
              and (:creadorId is null or p.creadorId = :creadorId)
              and (:moneda is null or upper(p.precio.moneda) = :moneda)
            order by p.nombre asc, p.id asc
~~~

Consulta 2:

~~~jpql
select count(p.id) from PrendaJpaEntity p
            where (:texto is null or lower(p.nombre) like :texto escape '!'
                or lower(p.descripcion) like :texto escape '!')
              and (:creadorId is null or p.creadorId = :creadorId)
              and (:moneda is null or upper(p.precio.moneda) = :moneda)
~~~

Consulta 3:

~~~jpql
select distinct p from PrendaJpaEntity p
            left join fetch p.variantes
            where p.id in :ids
            order by p.nombre asc, p.id asc
~~~

Consulta 4:

~~~jpql
select new com.uniquindio.ecommerce.domain.query.VariedadCreador(
                p.creadorId, count(v.id), count(distinct p.id))
            from PrendaJpaEntity p join p.variantes v
            where p.disponibleParaLaVenta = true
            group by p.creadorId
            order by count(v.id) desc, p.creadorId asc
~~~

Primero se paginan IDs sin fetch de colección. El COUNT usa los mismos predicados. Después se hace un fetch por esos IDs, sin Pageable, para cargar variantes de esa página. Evita paginación en memoria y multiplicación del padre por JOIN. Spring Data puede omitir COUNT si deduce el total de una página incompleta; no se promete siempre el mismo número de sentencias.

El ranking GET /api/reportes/creadores/variedad agrupa por creador. COUNT(v.id) cuenta variantes ofertadas; COUNT(DISTINCT p.id) cuenta prendas distintas. El JOIN interno excluye creadores sin variantes. Incluye stock cero y excluye prendas suspendidas y eliminados. No mide ventas ni unidades: una prenda con dos tallas cuenta dos variantes, aunque cada una tenga varias unidades.

@SoftDelete añade predicados SQL de prenda/variante; eliminado no es atributo JPQL inventado. Las pruebas capturan OFFSET/FETCH FIRST, activan fail_on_pagination_over_collection_fetch y comparan ranking con SQL independiente. Un caso con 13 prendas y página de 5 comprueba tres sentencias y ausencia del patrón N+1 en ese escenario. No se afirma rendimiento para cualquier volumen ni consistencia de una instantánea bajo toda escritura concurrente.

Evidencia de ejecución SQL: [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java) · ejecutaLimiteEnSqlYNumeroConstanteDeConsultasSinNMasUno() · línea 265; [docs/evidencias-2.6/verificacion.json](docs/evidencias-2.6/verificacion.json); [docs/evidencias-2.6/ranking-h2.html](docs/evidencias-2.6/ranking-h2.html).

## 9. Apartado 2.7 Seguridad JWT y BCrypt

### 9.1 Registro y contraseñas

AuthController.register recibe RegistroRequest; RegistrarUsuarioUseCase normaliza correo, exige contraseña válida y evita correo duplicado. BCryptContrasenas.codificar aplica BCryptPasswordEncoder con coste 12 y sal incorporada en el hash. Se guarda password_hash, nunca la contraseña plana. Usuario comprueba formato de hash; UsuarioMapper no lo expone. La restricción única del correo también protege persistencia.

IniciarSesionUseCase verifica con matches y rechaza credenciales incorrectas/cuenta inactiva con mensaje uniforme. Para un correo inexistente, BCryptContrasenas usa un hash ficticio para efectuar también trabajo BCrypt. No se presenta esto como una certificación general contra todos los canales temporales.

### 9.2 Emisión y verificación

NimbusEmisorTokens.emitir genera header alg HS256/type JWT y claims iss=boheme, sub=UUID, iat, exp, jti, correo y rol. No incluye contraseña ni hash. Un JWT tiene header.payload.signature en Base64URL: el payload es legible, no cifrado. HS256 usa una clave simétrica compartida para autenticar el contenido. Cambiar rol o sub invalida la firma; decodificar en jwt.io no demuestra verificación criptográfica.

JwtConfig exige secreto Base64 de al menos 32 bytes y duración entre un segundo y 24 horas; el valor normal es PT1H. El decoder restringe HS256, verifica timestamps sin tolerancia añadida, emisor y claims obligatorios. SecurityConfig vuelve a consultar la cuenta persistida para comprobar actividad, correo y rol. La aplicación es stateless respecto a sesión HTTP, aunque sí consulta la base de usuarios en cada petición.

No publicar secretos, claves ni JWT activos en estos documentos. Los ejemplos de la guía son cuentas ficticias locales. Para jwt.io usar únicamente un token sintético sin acceso a BOHEME; nunca la variable BOHEME_JWT_SECRET.

### 9.3 Roles y propiedad

La SecurityFilterChain desactiva formulario, basic y sesión; usa OAuth2 Resource Server. Registro/login, GET de prendas, ranking y recursos OpenAPI son públicos. POST/PATCH de prendas exigen VENDEDOR; bolsas y GET de pedidos, COMPRADOR. AutorizarRecursosUseCase valida identidad o carga propietario para operaciones antiguas; agregar/confirmar/consultar pedido lo hacen en el caso de compra.

401 significa no autenticado; 403, autenticado sin permiso de rol o propiedad. La existencia de un UUID válido no concede acceso. Swagger Authorize guarda un token en el navegador y añade Bearer; login no reemplaza automáticamente esa autorización. Logout en Swagger retira el token de la UI, no revoca el JWT en el backend.

No hay refresh token, lista de revocación individual ni endpoint de cambio de contraseña. Desactivar una cuenta invalida su uso por la comprobación persistida. H2 sólo se permite en perfil dev local.

Fuentes de seguridad: [src/main/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfig.java) · jwtDecoder() · línea 32; [src/main/java/com/uniquindio/ecommerce/infrastructure/security/NimbusEmisorTokens.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/NimbusEmisorTokens.java) · emitir() · línea 18; [src/main/java/com/uniquindio/ecommerce/infrastructure/security/BCryptContrasenas.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/BCryptContrasenas.java) · verificar() · línea 11; [src/main/java/com/uniquindio/ecommerce/infrastructure/security/SecurityConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/SecurityConfig.java) · apiSecurity() · línea 23; [src/test/java/com/uniquindio/ecommerce/infrastructure/security/SeguridadJwtIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/security/SeguridadJwtIntegrationTest.java) · tokenConFirmaInvalidaDevuelve401() · línea 173; [docs/evidencias-2.7/verificacion.json](docs/evidencias-2.7/verificacion.json).

## 10. Apartado 2.8 Pruebas automatizadas

JUnit ejecuta aserciones y casos parametrizados; cada invocación parametrizada cuenta como ejecución. Mockito sustituye colaboradores al probar delegación y respuestas. ApiUseCasesTest usa repositorios HashMap reales de prueba; ConsultasAvanzadasUseCasesTest usa mock/when/verify. No confundir mocks con persistencia real.

@WebMvcTest delimita controllers, mappers y handler; los casos de uso se sustituyen con @MockitoBean. Esas pruebas de contrato no demuestran por sí solas JWT real: SeguridadJwtIntegrationTest y CompraHttpIntegrationTest usan @SpringBootTest, MockMvc, filtros reales y H2. @DataJpaTest prueba adaptadores/consultas con flush/clear y rollback. @SpringBootTest también comprueba el contexto, seed, coordinación transaccional y concurrencia.

AAA significa preparar datos y colaboradores (Arrange), ejecutar la acción (Act) y comprobar resultados y efectos (Assert). Ejemplo: agregar dos unidades, intentar exceder el stock y comprobar excepción junto con cantidad/stock originales. El rollback de una prueba aislada no sustituye una prueba explícita del rollback de negocio: ésta también existe.

### 10.1 Conteos verificados en los XML disponibles

| Categoría exclusiva | Ejecuciones |
| --- | --- |
| Casos de uso unitarios | 11 |
| Dominio | 38 |
| Integración SpringBootTest | 70 |
| JPA DataJpaTest | 34 |
| Controllers WebMvcTest | 57 |
| Configuración seguridad unitaria | 9 |
| Total | 219 |

| Clase | Ejecuciones | Fallos | Errores | Omitidas |
| --- | --- | --- | --- | --- |
| ApiUseCasesTest | 4 | 0 | 0 | 0 |
| ConsultasAvanzadasUseCasesTest | 7 | 0 | 0 | 0 |
| BolsaDeEstiloTest | 2 | 0 | 0 | 0 |
| EntidadTest | 3 | 0 | 0 | 0 |
| InvariantesEntidadesTest | 21 | 0 | 0 | 0 |
| PedidoCompraTest | 2 | 0 | 0 | 0 |
| PrendaDeAutorTest | 2 | 0 | 0 | 0 |
| ReconstruccionAgregadosTest | 4 | 0 | 0 | 0 |
| ValueObjectTest | 4 | 0 | 0 | 0 |
| EcommerceApplicationTests | 1 | 0 | 0 | 0 |
| CasosDeUsoJpaIntegrationTest | 4 | 0 | 0 | 0 |
| ConsultasAvanzadasJpaTest | 19 | 0 | 0 | 0 |
| FlujoCompraIntegrationTest | 16 | 0 | 0 | 0 |
| PersistenciaJpaTest | 12 | 0 | 0 | 0 |
| UsuarioJpaTest | 3 | 0 | 0 | 0 |
| BolsaDeEstiloControllerTest | 10 | 0 | 0 | 0 |
| CompraHttpIntegrationTest | 12 | 0 | 0 | 0 |
| PrendaControllerTest | 45 | 0 | 0 | 0 |
| ReporteControllerTest | 2 | 0 | 0 | 0 |
| JwtConfigTest | 9 | 0 | 0 | 0 |
| SeguridadJwtIntegrationTest | 37 | 0 | 0 | 0 |

El informe histórico 2.8 conservó 189 ejecuciones en 18 clases; no añadió una batería nueva y sólo hizo explícito el smoke test del contexto. El apartado 2.9 añadió 30 ejecuciones: PedidoCompraTest (2), FlujoCompraIntegrationTest (16), CompraHttpIntegrationTest (12), para 219 en 21 clases. No son 219 métodos distintos ni 219 pruebas exclusivas de compra. No hay medición porcentual de cobertura que permita afirmar 100% de cobertura.

La última auditoría de la conversación ejecutó .\gradlew.bat clean test con BUILD SUCCESSFUL. En esta tarea se leyeron los XML disponibles y se contrastaron con las clases, sin una nueva corrida. Reporte navegable: build/reports/tests/test/index.html; XML: build/test-results/test/TEST-*.xml. El comando clean regenera esos archivos; las evidencias permanentes de etapas anteriores permanecen en docs.

Evidencias permanentes: [docs/evidencias-2.8/resumen-pruebas.json](docs/evidencias-2.8/resumen-pruebas.json), [docs/evidencias-2.9/pruebas.json](docs/evidencias-2.9/pruebas.json), [docs/evidencias-2.9/gradle-clean-test.log](docs/evidencias-2.9/gradle-clean-test.log). Las fechas UTC de XML actuales van de 2026-10-25T20:07:34.357Z a 2026-10-25T20:08:02.552Z.

## 11. Apartado 2.9 Flujo completo del negocio

### 11.1 Recorrido y validaciones

1. Vendedor autentica con POST login (200) y autoriza su JWT.
2. POST prendas valida DTO/propietario, crea el dominio y persiste (201).
3. Comprador autentica (200), cambia JWT y crea bolsa propia (201).
4. POST items bloquea bolsa/prenda, valida propietario, estado, cantidad, existencia y oferta. agregarLinea valida moneda y stock acumulado. Devuelve bolsa/subtotales (200) y no reserva inventario.
5. POST confirmar-pedido vuelve a validar propietario, bolsa activa no vacía, catálogo, oferta y stock real. Bloquea prendas distintas ordenadas por UUID, descuenta variantes y construye líneas con el precio elegido en la bolsa.
6. En una transacción guarda inventario, cierra bolsa y registra pedido con flush (201 y Location). El total se calcula en dominio; el precio no se toma del cuerpo del comprador.
7. GET pedidos devuelve el histórico sólo al comprador propietario (200); ajeno recibe 403 y ausente 404.

### 11.2 Secuencia basada en las clases reales

~~~mermaid
sequenceDiagram
  actor V as Vendedor
  actor B as Comprador
  participant Auth as AuthController
  participant Login as IniciarSesionUseCase
  participant PC as PrendaController
  participant PM as PrendaMapper
  participant Reg as RegistrarPrendaDeAutorUseCase
  participant BC as BolsaDeEstiloController
  participant Crear as CrearBolsaDeEstiloUseCase
  participant CC as CompraController
  participant Add as AgregarVarianteABolsaUseCase
  participant Conf as ConfirmarPedidoUseCase
  participant Consultar as ConsultarPedidoUseCase
  participant JPA as Adaptadores JPA
  participant H2 as H2
  V->>Auth: POST login
  Auth->>Login: ejecutar(correo,password)
  Login-->>V: JWT vendedor
  V->>PC: POST prendas con Bearer
  PC->>PM: toDomain(request)
  PM-->>PC: PrendaDeAutor validada
  PC->>Reg: ejecutar(prenda)
  Reg->>JPA: guardarPrenda
  JPA->>H2: INSERT raíz y variantes
  PC-->>V: 201 y IDs
  B->>Auth: POST login
  Auth->>Login: ejecutar(correo,password)
  Login-->>B: JWT comprador
  B->>BC: POST bolsas
  BC->>Crear: ejecutar(clienteId)
  Crear->>JPA: guardarBolsa
  BC-->>B: 201 y bolsaId
  B->>CC: POST items
  CC->>Add: ejecutar(bolsaId,varianteId,cantidad)
  Add->>JPA: bloquear bolsa/prenda y guardar selección
  CC-->>B: 200 y subtotal
  B->>CC: POST confirmar-pedido
  CC->>Conf: ejecutar(bolsaId)
  Note over Conf,H2: Transacción de confirmación
  Conf->>JPA: bloquear bolsa y prendas en orden
  Conf->>Conf: revalidar stock y crear histórico
  Conf->>JPA: guardar stock, cerrar bolsa, registrar pedido
  JPA->>H2: UPDATE e INSERT con flush
  CC-->>B: 201 PedidoResponse y Location
  B->>CC: GET pedidos por ID
  CC->>Consultar: ejecutar(pedidoId)
  Consultar->>JPA: porId y verificar propietario
  CC-->>B: 200 histórico
~~~

### 11.3 Atomicidad, duplicados y concurrencia

ConfirmarPedidoUseCase.ejecutar usa @Transactional. Los adaptadores participan en esa transacción. PESSIMISTIC_WRITE serializa modificaciones de bolsa y de raíz prenda. Ordenar UUID de prendas evita ciclos por dos bolsas con artículos en orden inverso. Esto es un mecanismo comprobado en H2, no una prueba de carga ni una garantía para todo motor futuro.

La bolsa cerrada rechaza otra compra; uk_pedido_bolsa impone unicidad en base. Las compras concurrentes de una misma bolsa producen un pedido y un descuento. Dos compradores que juntos exceden stock producen una compra y un rechazo. El agregado no descuenta parcialmente una compra que falla.

La prueba falloDespuesDeFlushReviertePedidoInventarioYBolsa llama al método real registrar, efectúa INSERT/UPDATE/flush y provoca excepción: comprueba cero cabeceras/líneas, stock original y bolsa abierta. No se limita a fallar antes de escribir. Otra prueba falla por una línea sin stock y comprueba que no queda descuento parcial.

ConservaPrecioHistorico prueba un cambio de precio después de agregar y un soft delete posterior: el pedido conserva el precio de la selección. El modelo no actualiza automáticamente una bolsa al precio futuro del catálogo; esa es una decisión explícita.

### 11.4 Evidencias HTTP y SQL

Los JSON históricos de 2.9 contienen 23 peticiones Newman y 33 aserciones correctas, cero fallos. Se revisaron sus cifras, no se ejecutó Newman de nuevo para este documento. Son una demostración alternativa histórica; el video previsto utilizará Swagger. Ocho SELECT de la misma JVM muestran antes del éxito cero pedidos, stock 3 y bolsa abierta; después, un pedido PENDIENTE de dos unidades, total 39,90 COP, stock 1 y bolsa cerrada.

| Comprobación | Método de prueba real |
| --- | --- |
| Persistencia y descuento | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · pedidoPersistePendienteDescuentaVariantesCierraBolsaYSeRecupera() · línea 113 |
| Stock al confirmar | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · revalidaStockYEliminacionAlConfirmar() · línea 149 |
| Rollback posterior a flush | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · falloDespuesDeFlushReviertePedidoInventarioYBolsa() · línea 159 |
| Rollback de varias líneas | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · falloDeUnaLineaNoDescuentaParcialmenteLasOtras() · línea 220 |
| Sobreventa concurrente | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · dosCompradoresConcurrentesNoSobrevendenLaUltimaExistencia() · línea 197 |
| Bolsa concurrente | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · confirmacionesConcurrentesDeMismaBolsaCreanUnSoloPedido() · línea 209 |
| Orden de bloqueos | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · comprasConcurrentesConLineasEnOrdenInversoUsanBloqueoEstable() · línea 233 |
| Precio histórico | [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) · conservaPrecioHistoricoAunqueCambieCatalogoAntesYDespuesDeConfirmar() · línea 128 |

Evidencia [docs/evidencias-2.9/error.json](docs/evidencias-2.9/error.json): 10 peticiones, 14 aserciones; fecha UTC 2026-10-25T03:13:48.210709+00:00.

Evidencia [docs/evidencias-2.9/final.json](docs/evidencias-2.9/final.json): 6 peticiones, 11 aserciones; fecha UTC 2026-10-25T03:13:52.004899+00:00.

Evidencia [docs/evidencias-2.9/seguridad.json](docs/evidencias-2.9/seguridad.json): 7 peticiones, 8 aserciones; fecha UTC 2026-10-25T03:13:56.798636+00:00.

## 12. Verificación frente a la rúbrica

La matriz se basa en los ocho criterios y pesos proporcionados por el usuario; no se dispone aquí de un documento oficial adicional que permita certificar condiciones no transcritas. Implementado y verificado significa código y evidencia concreta, no calificación otorgada.

| Criterio | Puntos | Estado | Qué mostrar | Momento |
| --- | --- | --- | --- | --- |
| Correcciones Entrega 1 | 10 | Implementado y verificado; registro Git final pendiente | Prueba reflexión e invariantes; diff local | Bloque 2, 0:45–2:30 |
| Dominio implementado | 15 | Implementado y verificado | Raíz, fábricas, identidad, Dinero y reglas | Bloque 3, 2:30–5:00 |
| Casos de uso | 15 | Implementado y verificado | RegistrarPrenda y AgregarVariante/Confirmar; pruebas por capa | Bloque 4, 5:00–7:30 |
| API REST | 15 | Implementado y verificado por HTTP/OpenAPI; visual pendiente | 15 rutas y DTO; Swagger Authorize/Execute manual | Bloques 4, 6 y 7 |
| Persistencia y consultas | 15 | Implementado y verificado | H2, mappers, JPQL, SQL real y DataJpaTest | Bloque 5, 7:30–10:00 |
| Seguridad | 15 | Implementado y verificado; demo visual pendiente | BCrypt, HS256, roles, propiedad y 401/403/200 | Bloque 6, 10:00–12:30 |
| Pruebas | 10 | Implementado y verificado en resultados disponibles | 219/219, 21 clases, 0 omitidas | Bloque 8, 14:15–15:00 |
| Organización Git y README | 5 | Parcial; README final y evidencia colaborativa pendientes | main/origin/main, commits reales; no inventar PR | Bloque 8, 14:15–15:00 |

| Criterio del video | Demostración prevista | Estado actual |
| --- | --- | --- |
| Decisiones | Elegir separación dominio/JPA y bloqueo transaccional, bloques 3/5/8 | Guion preparado; video pendiente |
| Dominio y agregados | Bolsa/Prenda, límites e identidad, bloque 3 | Código verificado; exposición pendiente |
| Flujo por capas | Controller, DTO, mapper, caso, dominio y repo, bloque 4 | Código verificado |
| Persistencia y consultas | Tablas y JPQL/SQL, bloque 5 | Evidencias disponibles |
| Seguridad | Login, JWT sintético y 401/403/200, bloque 6 | HTTP verificado; UI pendiente |
| Demo completa | Recorrido de compra y error, bloque 7 | Pruebas y HTTP históricos; grabación pendiente |
| Correcciones | Visibilidad ya privada y diff de reglas, bloque 2 | Sin commit final de correcciones |
| Participación equitativa | Asignar cada bloque y registrar minutos por persona | Pendiente nombres y reparto |

No aplica al alcance actual: cobro real, logística, frontend y MariaDB operativa. Pendientes: README raíz, ramas/PR y contribuciones confirmadas, permisos de acceso al repositorio/video, grabación de máximo 15 minutos y revisión manual de Swagger. Las pruebas existentes de rollback/concurrencia pueden apoyar discusión opcional; no se implementa ni documenta el trabajo independiente de seguimiento de pruebas avanzadas.

## 13. Decisiones arquitectónicas

| Decisión | Motivo | Alternativa | Limitación |
| --- | --- | --- | --- |
| Dominio separado de JPA | Conservar reglas y pruebas sin Hibernate | Una sola entidad con anotaciones ORM | Más clases y mappers; modelos antiguos no totalmente homogéneos |
| DTO/mappers HTTP | Contrato explícito y no exponer hash/filas | Serializar dominio/JPA directamente | Hay acoplamiento de LoginResponse en aplicación |
| Dinero BigDecimal y escala | No redondear valores válidos | double o DECIMAL fijo a dos cifras | DECFLOAT específico de H2 requiere migración cuidadosa |
| IDs y fetch en dos pasos | Paginar en SQL sin fetch de colección | Cargar todo y paginar streams | Varias consultas, no snapshot inmutable garantizado |
| Ranking de oferta | Datos reales de catálogo disponibles | Inventar ranking de ventas | No mide ventas y cuenta stock cero |
| HS256 y BCrypt coste 12 | Firma e integridad del token; password no recuperable | Sesiones o firma asimétrica | Clave simétrica, sin refresh/revocación individual |
| Bloqueo pesimista ordenado | Evitar sobreventa y doble compra | Sólo comprobar stock sin bloqueo | Contención por raíz y pruebas sólo sobre H2 |
| Precio histórico de bolsa | Respetar selección y no depender del catálogo futuro | Repreciar siempre al confirmar | Precio puede diferir del catálogo actual |
| H2/create-drop y seed dev | Reproducción sencilla para demostración | Persistencia de disco o MariaDB | Sin durabilidad entre reinicios |
| Compatibilidad del dominio antiguo | Conservar API y pruebas originales | Eliminar clases/transiciones no usadas | Conviven VarianteDeTalla/VariantePrenda y dos formas de pedido |

## 14. Conclusiones

BOHEME cuenta con un backend demostrable de punta a punta y reglas observables en dominio, HTTP y base de datos. La confirmación crea pedido e histórico, descuenta inventario y cierra bolsa de forma atómica, con pruebas de rechazo, rollback y concurrencia. Las 219 ejecuciones y las evidencias históricas no sustituyen la sustentación: todavía hay que mostrar decisiones, acciones y resultados con nuestras palabras.

El cierre académico requiere completar README, registrar cambios de forma acordada en Git, validar acceso al repositorio y video, asignar participación y grabar sin superar quince minutos. No se hizo commit/push ni se alteró código en esta tarea. Una etapa posterior podrá abordar migración a MariaDB, frontend, reservas, pagos y logística, con reglas y pruebas nuevas cuando ese alcance sea autorizado.

### Anexo A Fuentes y diferencias entre etapas

Los informes de etapas son históricos: el 2.5 decía que no había usuarios/pedidos y el 2.6 contaba diez operaciones; esos datos eran propios de su fecha. La configuración actual desactiva seed fuera de dev, aunque un informe antiguo describa otro valor. El mapeo API actual ya enumera quince rutas. El glosario contiene un enlace externo cuyo contenido no está almacenado: no se utilizó como evidencia de requisitos no visibles.

- [docs/api/mapeo-dominio.api.md](docs/api/mapeo-dominio.api.md)

- [docs/persistencia-jpa.md](docs/persistencia-jpa.md)

- [docs/consultas-avanzadas-2.6.md](docs/consultas-avanzadas-2.6.md)

- [docs/seguridad-jwt-2.7.md](docs/seguridad-jwt-2.7.md)

- [docs/pruebas-automatizadas-2.8.md](docs/pruebas-automatizadas-2.8.md)

- [docs/flujo-negocio-2.9.md](docs/flujo-negocio-2.9.md)

- [docs/diagrama-agregado.md](docs/diagrama-agregado.md)

- [docs/glosario-lenguaje-ubicuo.md](docs/glosario-lenguaje-ubicuo.md)

- [docs/evidencias-2.8/arquitectura-y-h2.json](docs/evidencias-2.8/arquitectura-y-h2.json)

- [docs/evidencias-2.9/archivos-fuente.json](docs/evidencias-2.9/archivos-fuente.json)

Los scripts auxiliares existentes son clientes de evidencia externos: verificar-consultas-avanzadas.py, verificar-seguridad-jwt.py y evidencia-flujo-2.9.py. Pueden escribir reportes y mutar datos ficticios; no deben ejecutarse durante una demostración exclusivamente Swagger ni tratarse como dependencias del backend. La colección Postman y sus entornos son evidencia alternativa, no el recorrido del video.

### Anexo B Estado de Git al iniciar la documentación

HEAD: 9230eb1 2026-10-17 17:46:30 -0500 se añade services y config. Rama local main y referencia remota origin/main en el mismo commit. Remoto configurado: https://github.com/sofism01/ecommerce-PA. No se consultaron PR o permisos remotos. Se encontraron 29 archivos versionados modificados, 210 nuevos sin seguimiento y cero archivos preparados. Los tres documentos de esta tarea se añaden después de esa fotografía. No hay README en la raíz en el estado inspeccionado.

~~~text
9230eb1 | 2026-10-17 | se añade services y config
c7d9e85 | 2026-10-17 | fix: implementacion en memoria repository
bf027ff | 2026-10-17 | creacion de los dtos de request y response (laura)
fa7c156 | 2026-10-17 | modificaciones de los casos de uso
2e53edb | 2026-10-17 | implementar caso de uso ObtenerBolsaDeEstiloActiva (laura)
a6368b9 | 2026-10-17 | implementar caso de uso vaciarBolsaDeEstilo
4943cc5 | 2026-10-17 | implementar ListarPrendasPorCreador caso de uso (laura)
d964e9a | 2026-10-17 | implementacion de caso de uso SuspenderVentaPrendaDeAutor
3f76ba1 | 2026-10-17 | implementacion de caso de uso CerrarBolsaDeEstilo (laura)
7cbbb2e | 2026-10-17 | implementacion de caso de uso ActualizarStockVariantePrenda
f0f6fb0 | 2026-10-07 | correccion caso de uso RegistrarPrendaDeAutorUseCase
0a37fbc | 2026-10-04 | update aggregate diagram with invariants and refined parameters
~~~

Los mensajes que mencionan una persona no certifican por sí solos participación equitativa ni autoría de todo un módulo. El equipo debe completar nombres y responsabilidades con evidencia acordada. No hay ramas de trabajo adicionales visibles en el inventario local; no se afirma que existan PR. Los cambios actuales todavía no pueden mostrarse como commits terminados.

### Anexo C Inventario de producción y pruebas

Producción: 123 archivos Java inspeccionados. El inventario sirve para navegación; la explicación de reglas y evidencias está en las secciones anteriores.

| Archivo | Paquete relativo |
| --- | --- |
| [src/main/java/com/uniquindio/ecommerce/EcommerceApplication.java](src/main/java/com/uniquindio/ecommerce/EcommerceApplication.java) | . |
| [src/main/java/com/uniquindio/ecommerce/application/dto/request/ActualizarStockRequest.java](src/main/java/com/uniquindio/ecommerce/application/dto/request/ActualizarStockRequest.java) | application\dto\request |
| [src/main/java/com/uniquindio/ecommerce/application/dto/request/AgregarItemBolsaRequest.java](src/main/java/com/uniquindio/ecommerce/application/dto/request/AgregarItemBolsaRequest.java) | application\dto\request |
| [src/main/java/com/uniquindio/ecommerce/application/dto/request/CrearBolsaRequest.java](src/main/java/com/uniquindio/ecommerce/application/dto/request/CrearBolsaRequest.java) | application\dto\request |
| [src/main/java/com/uniquindio/ecommerce/application/dto/request/LoginRequest.java](src/main/java/com/uniquindio/ecommerce/application/dto/request/LoginRequest.java) | application\dto\request |
| [src/main/java/com/uniquindio/ecommerce/application/dto/request/RegistrarPrendaRequest.java](src/main/java/com/uniquindio/ecommerce/application/dto/request/RegistrarPrendaRequest.java) | application\dto\request |
| [src/main/java/com/uniquindio/ecommerce/application/dto/request/RegistroRequest.java](src/main/java/com/uniquindio/ecommerce/application/dto/request/RegistroRequest.java) | application\dto\request |
| [src/main/java/com/uniquindio/ecommerce/application/dto/request/VariantePrendaRequest.java](src/main/java/com/uniquindio/ecommerce/application/dto/request/VariantePrendaRequest.java) | application\dto\request |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/BolsaDeEstiloResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/BolsaDeEstiloResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/DineroResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/DineroResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/ErrorResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/ErrorResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/LineaDeBolsaResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/LineaDeBolsaResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/LineaPedidoResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/LineaPedidoResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/LoginResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/LoginResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/PaginaPrendasResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/PaginaPrendasResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/PedidoResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/PedidoResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/PrendaResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/PrendaResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/UsuarioResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/UsuarioResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/VariantePrendaResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/VariantePrendaResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/dto/response/VariedadCreadorResponse.java](src/main/java/com/uniquindio/ecommerce/application/dto/response/VariedadCreadorResponse.java) | application\dto\response |
| [src/main/java/com/uniquindio/ecommerce/application/security/AccesoDenegadoException.java](src/main/java/com/uniquindio/ecommerce/application/security/AccesoDenegadoException.java) | application\security |
| [src/main/java/com/uniquindio/ecommerce/application/security/CodificadorContrasenas.java](src/main/java/com/uniquindio/ecommerce/application/security/CodificadorContrasenas.java) | application\security |
| [src/main/java/com/uniquindio/ecommerce/application/security/CredencialesInvalidasException.java](src/main/java/com/uniquindio/ecommerce/application/security/CredencialesInvalidasException.java) | application\security |
| [src/main/java/com/uniquindio/ecommerce/application/security/EmisorTokens.java](src/main/java/com/uniquindio/ecommerce/application/security/EmisorTokens.java) | application\security |
| [src/main/java/com/uniquindio/ecommerce/application/security/UsuarioActual.java](src/main/java/com/uniquindio/ecommerce/application/security/UsuarioActual.java) | application\security |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ActualizarStockVariantePrendaUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ActualizarStockVariantePrendaUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/AgregarVarianteABolsaUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/AgregarVarianteABolsaUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/AutorizarRecursosUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/AutorizarRecursosUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/BuscarPrendasUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/BuscarPrendasUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/CerrarBolsaDeEstiloUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/CerrarBolsaDeEstiloUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ConfirmarPedidoUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ConfirmarPedidoUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarPedidoUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarPedidoUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarVariedadCreadoresUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ConsultarVariedadCreadoresUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/CrearBolsaDeEstiloUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/CrearBolsaDeEstiloUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/IniciarSesionUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/IniciarSesionUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ListarPrendasPorCreadorUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ListarPrendasPorCreadorUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/ObtenerBolsaDeEstiloActivaUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/ObtenerBolsaDeEstiloActivaUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarPrendaDeAutorUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarPrendaDeAutorUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarUsuarioUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/RegistrarUsuarioUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/SuspenderVentaPrendaDeAutorUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/SuspenderVentaPrendaDeAutorUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/application/usecase/VaciarBolsaDeEstiloUseCase.java](src/main/java/com/uniquindio/ecommerce/application/usecase/VaciarBolsaDeEstiloUseCase.java) | application\usecase |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstilo.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Creador.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PedidoDeModa.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java](src/main/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutor.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/Usuario.java](src/main/java/com/uniquindio/ecommerce/domain/entity/Usuario.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VarianteDeTalla.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VariantePrenda.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java](src/main/java/com/uniquindio/ecommerce/domain/entity/VitrinaDeCreador.java) | domain\entity |
| [src/main/java/com/uniquindio/ecommerce/domain/exception/BolsaDeEstiloNoEncontradaException.java](src/main/java/com/uniquindio/ecommerce/domain/exception/BolsaDeEstiloNoEncontradaException.java) | domain\exception |
| [src/main/java/com/uniquindio/ecommerce/domain/exception/CorreoDuplicadoException.java](src/main/java/com/uniquindio/ecommerce/domain/exception/CorreoDuplicadoException.java) | domain\exception |
| [src/main/java/com/uniquindio/ecommerce/domain/exception/ModificacionBolsaNoPermitidaException.java](src/main/java/com/uniquindio/ecommerce/domain/exception/ModificacionBolsaNoPermitidaException.java) | domain\exception |
| [src/main/java/com/uniquindio/ecommerce/domain/exception/RecursoNoEncontradoException.java](src/main/java/com/uniquindio/ecommerce/domain/exception/RecursoNoEncontradoException.java) | domain\exception |
| [src/main/java/com/uniquindio/ecommerce/domain/exception/ReglaDominioException.java](src/main/java/com/uniquindio/ecommerce/domain/exception/ReglaDominioException.java) | domain\exception |
| [src/main/java/com/uniquindio/ecommerce/domain/exception/StockInsuficienteException.java](src/main/java/com/uniquindio/ecommerce/domain/exception/StockInsuficienteException.java) | domain\exception |
| [src/main/java/com/uniquindio/ecommerce/domain/query/ConsultaInvalidaException.java](src/main/java/com/uniquindio/ecommerce/domain/query/ConsultaInvalidaException.java) | domain\query |
| [src/main/java/com/uniquindio/ecommerce/domain/query/ConsultaPrendas.java](src/main/java/com/uniquindio/ecommerce/domain/query/ConsultaPrendas.java) | domain\query |
| [src/main/java/com/uniquindio/ecommerce/domain/query/Pagina.java](src/main/java/com/uniquindio/ecommerce/domain/query/Pagina.java) | domain\query |
| [src/main/java/com/uniquindio/ecommerce/domain/query/VariedadCreador.java](src/main/java/com/uniquindio/ecommerce/domain/query/VariedadCreador.java) | domain\query |
| [src/main/java/com/uniquindio/ecommerce/domain/repository/BolsaDeEstiloRepository.java](src/main/java/com/uniquindio/ecommerce/domain/repository/BolsaDeEstiloRepository.java) | domain\repository |
| [src/main/java/com/uniquindio/ecommerce/domain/repository/CatalogoVariantesRepository.java](src/main/java/com/uniquindio/ecommerce/domain/repository/CatalogoVariantesRepository.java) | domain\repository |
| [src/main/java/com/uniquindio/ecommerce/domain/repository/ConsultaPrendasRepository.java](src/main/java/com/uniquindio/ecommerce/domain/repository/ConsultaPrendasRepository.java) | domain\repository |
| [src/main/java/com/uniquindio/ecommerce/domain/repository/PedidoDeModaRepository.java](src/main/java/com/uniquindio/ecommerce/domain/repository/PedidoDeModaRepository.java) | domain\repository |
| [src/main/java/com/uniquindio/ecommerce/domain/repository/PrendaDeAutorRepository.java](src/main/java/com/uniquindio/ecommerce/domain/repository/PrendaDeAutorRepository.java) | domain\repository |
| [src/main/java/com/uniquindio/ecommerce/domain/repository/UsuarioRepository.java](src/main/java/com/uniquindio/ecommerce/domain/repository/UsuarioRepository.java) | domain\repository |
| [src/main/java/com/uniquindio/ecommerce/domain/service/ActualizadorDeStock.java](src/main/java/com/uniquindio/ecommerce/domain/service/ActualizadorDeStock.java) | domain\service |
| [src/main/java/com/uniquindio/ecommerce/domain/service/GestorDeBolsaDeEstilo.java](src/main/java/com/uniquindio/ecommerce/domain/service/GestorDeBolsaDeEstilo.java) | domain\service |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/BolsaDeEstiloId.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/BolsaDeEstiloId.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/Dinero.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/Dinero.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/EstadoPedido.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/EstadoPedido.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDeBolsa.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDeBolsa.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDePedido.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/LineaDePedido.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/MedidasCorporales.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/MedidasCorporales.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/PrendaDeAutorId.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/PrendaDeAutorId.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/Rol.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/Rol.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/SKU.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/SKU.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/TipoTalla.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/TipoTalla.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/domain/valueobject/VariantePrendaId.java](src/main/java/com/uniquindio/ecommerce/domain/valueobject/VariantePrendaId.java) | domain\valueobject |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/config/DomainServiceConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/config/DomainServiceConfig.java) | infrastructure\config |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/config/JsonConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/config/JsonConfig.java) | infrastructure\config |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/config/OpenApiConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/config/OpenApiConfig.java) | infrastructure\config |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/InMemoryBolsaDeEstiloRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/InMemoryBolsaDeEstiloRepository.java) | infrastructure\persistence\jpa |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/InMemoryPrendaDeAutorRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/InMemoryPrendaDeAutorRepository.java) | infrastructure\persistence\jpa |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaBolsaDeEstiloRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaBolsaDeEstiloRepository.java) | infrastructure\persistence\jpa |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaPedidoDeModaRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaPedidoDeModaRepository.java) | infrastructure\persistence\jpa |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaPrendaDeAutorRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaPrendaDeAutorRepository.java) | infrastructure\persistence\jpa |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaUsuarioRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaUsuarioRepository.java) | infrastructure\persistence\jpa |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/BolsaJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/BolsaJpaEntity.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/DineroJpa.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/DineroJpa.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaBolsaJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaBolsaJpaEntity.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaPedidoJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/LineaPedidoJpaEntity.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PedidoJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PedidoJpaEntity.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PrendaJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/PrendaJpaEntity.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/UsuarioJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/UsuarioJpaEntity.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/VarianteJpaEntity.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/entity/VarianteJpaEntity.java) | infrastructure\persistence\jpa\entity |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/BolsaJpaMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/BolsaJpaMapper.java) | infrastructure\persistence\jpa\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/DineroJpaMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/DineroJpaMapper.java) | infrastructure\persistence\jpa\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/PedidoJpaMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/PedidoJpaMapper.java) | infrastructure\persistence\jpa\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/PrendaJpaMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/PrendaJpaMapper.java) | infrastructure\persistence\jpa\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/UsuarioJpaMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/mapper/UsuarioJpaMapper.java) | infrastructure\persistence\jpa\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/BolsaJpaRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/BolsaJpaRepository.java) | infrastructure\persistence\jpa\repository |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/PedidoJpaRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/PedidoJpaRepository.java) | infrastructure\persistence\jpa\repository |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/PrendaJpaRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/PrendaJpaRepository.java) | infrastructure\persistence\jpa\repository |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/UsuarioJpaRepository.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/UsuarioJpaRepository.java) | infrastructure\persistence\jpa\repository |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/seed/DatosIniciales.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/seed/DatosIniciales.java) | infrastructure\persistence\jpa\seed |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/seed/UsuariosDemo.java](src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/seed/UsuariosDemo.java) | infrastructure\persistence\jpa\seed |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/AuthController.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/AuthController.java) | infrastructure\rest\controller |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/BolsaDeEstiloController.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/BolsaDeEstiloController.java) | infrastructure\rest\controller |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraController.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraController.java) | infrastructure\rest\controller |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaController.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaController.java) | infrastructure\rest\controller |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/ReporteController.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/ReporteController.java) | infrastructure\rest\controller |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/exception/ApiExceptionHandler.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/exception/ApiExceptionHandler.java) | infrastructure\rest\exception |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/BolsaDeEstiloMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/BolsaDeEstiloMapper.java) | infrastructure\rest\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/ConsultaPrendasMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/ConsultaPrendasMapper.java) | infrastructure\rest\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/DineroMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/DineroMapper.java) | infrastructure\rest\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/PedidoMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/PedidoMapper.java) | infrastructure\rest\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/PrendaMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/PrendaMapper.java) | infrastructure\rest\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/UsuarioMapper.java](src/main/java/com/uniquindio/ecommerce/infrastructure/rest/mapper/UsuarioMapper.java) | infrastructure\rest\mapper |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/security/BCryptContrasenas.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/BCryptContrasenas.java) | infrastructure\security |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfig.java) | infrastructure\security |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/security/NimbusEmisorTokens.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/NimbusEmisorTokens.java) | infrastructure\security |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/security/SecurityConfig.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/SecurityConfig.java) | infrastructure\security |
| [src/main/java/com/uniquindio/ecommerce/infrastructure/security/SecurityContextUsuarioActual.java](src/main/java/com/uniquindio/ecommerce/infrastructure/security/SecurityContextUsuarioActual.java) | infrastructure\security |

Pruebas: 21 archivos Java inspeccionados. El inventario sirve para navegación; la explicación de reglas y evidencias está en las secciones anteriores.

| Archivo | Paquete relativo |
| --- | --- |
| [src/test/java/com/uniquindio/ecommerce/EcommerceApplicationTests.java](src/test/java/com/uniquindio/ecommerce/EcommerceApplicationTests.java) | . |
| [src/test/java/com/uniquindio/ecommerce/application/usecase/ApiUseCasesTest.java](src/test/java/com/uniquindio/ecommerce/application/usecase/ApiUseCasesTest.java) | application\usecase |
| [src/test/java/com/uniquindio/ecommerce/application/usecase/ConsultasAvanzadasUseCasesTest.java](src/test/java/com/uniquindio/ecommerce/application/usecase/ConsultasAvanzadasUseCasesTest.java) | application\usecase |
| [src/test/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstiloTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/BolsaDeEstiloTest.java) | domain\entity |
| [src/test/java/com/uniquindio/ecommerce/domain/entity/EntidadTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/EntidadTest.java) | domain\entity |
| [src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/InvariantesEntidadesTest.java) | domain\entity |
| [src/test/java/com/uniquindio/ecommerce/domain/entity/PedidoCompraTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/PedidoCompraTest.java) | domain\entity |
| [src/test/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutorTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/PrendaDeAutorTest.java) | domain\entity |
| [src/test/java/com/uniquindio/ecommerce/domain/entity/ReconstruccionAgregadosTest.java](src/test/java/com/uniquindio/ecommerce/domain/entity/ReconstruccionAgregadosTest.java) | domain\entity |
| [src/test/java/com/uniquindio/ecommerce/domain/valueobject/ValueObjectTest.java](src/test/java/com/uniquindio/ecommerce/domain/valueobject/ValueObjectTest.java) | domain\valueobject |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/CasosDeUsoJpaIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/CasosDeUsoJpaIntegrationTest.java) | infrastructure\persistence\jpa |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java) | infrastructure\persistence\jpa |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/FlujoCompraIntegrationTest.java) | infrastructure\persistence\jpa |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java) | infrastructure\persistence\jpa |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/UsuarioJpaTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/UsuarioJpaTest.java) | infrastructure\persistence\jpa |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/BolsaDeEstiloControllerTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/BolsaDeEstiloControllerTest.java) | infrastructure\rest\controller |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraHttpIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraHttpIntegrationTest.java) | infrastructure\rest\controller |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaControllerTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaControllerTest.java) | infrastructure\rest\controller |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/ReporteControllerTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/ReporteControllerTest.java) | infrastructure\rest\controller |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfigTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/security/JwtConfigTest.java) | infrastructure\security |
| [src/test/java/com/uniquindio/ecommerce/infrastructure/security/SeguridadJwtIntegrationTest.java](src/test/java/com/uniquindio/ecommerce/infrastructure/security/SeguridadJwtIntegrationTest.java) | infrastructure\security |

