# BOHEME Seguimiento de pruebas avanzadas del backend

Programación Avanzada · Universidad del Quindío · Docente Valentina Oviedo Sánchez.

## Objetivo y alcance

Completar los puntos 0, 1, 2, 3 y 4 de la actividad independiente **Seguimiento: Pruebas avanzadas del backend**, sobre el mismo BOHEME del Entregable 2. Se reutilizan pruebas que ya demuestran los requisitos y se añaden sólo cinco escenarios faltantes. No se modifican producción, dominio, endpoints, configuración, dependencias ni documentos anteriores. El README raíz sólo recibe una sección nueva al final.

La referencia normativa es la transcripción de requisitos suministrada por el usuario. No se encontró un PDF adjunto en el repositorio: el cotejo literal con ese archivo original no se presenta como realizado. No se requiere preparar Word, diapositivas ni video para esta actividad.

## 1. Auditoría inicial anterior a las modificaciones

Se inspeccionaron las 21 clases de pruebas, sus fixtures, anotaciones, aserciones, dobles y métodos, además de build.gradle y la configuración de seed. El inventario previo está en [auditoria-inicial.json](auditoria-inicial.json). La matriz se comunicó antes de editar el repositorio.

| Punto solicitado | Prueba existente | Archivo | Método | Cumple completamente al inicio | Qué faltaba |
| --- | --- | --- | --- | --- | --- |
| 0.1 Guardar/releer | Persistencia de PrendaDeAutor | [PersistenciaJpaTest](../../src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java) | guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado | Sí | Sólo trazabilidad para el seguimiento |
| 0.2 Ranking | JOIN/GROUP BY con datos controlados | [ConsultasAvanzadasJpaTest](../../src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java) | rankingCuentaVariantesYPrendasDistintasCoincideConSqlReal y filtros | Sí, como conjunto | Localizar los métodos de filtros |
| 1 Seguridad | Tokens y códigos reales en casos separados | [SeguridadJwtIntegrationTest](../../src/test/java/com/uniquindio/ecommerce/infrastructure/security/SeguridadJwtIntegrationTest.java) | todasLasRutasProtegidasExigenTokenYRol; compradorOperaSuBolsaYNoPuedeSuplantarCliente | Parcial | Una misma URL con un mismo recurso concreto en los tres escenarios |
| 2 Slice JPA | @DataJpaTest y adaptadores reales | [PersistenciaJpaTest](../../src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java) | guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado | Sí | Comparación académica y explicación del paquete de Boot 4 |
| 3 Integración | Seguridad, HTTP, casos, JPA y H2 | [CompraHttpIntegrationTest](../../src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraHttpIntegrationTest.java) | flujoCompletoConErrorIntencionalStockPedidoYRelecturaDesdeH2 | Sí | Documentar y comprobar ejecución aislada |
| 4 Errores sin guardar | 404/409 con casos simulados | [PrendaControllerTest](../../src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaControllerTest.java) | suspender_Inexistente_Devuelve404; suspender_ReglaIncumplida_Devuelve409 | No para el requisito del repositorio | Caso real y verify(..., never()) sobre el repositorio que participa |

### Clasificación encontrada

- **@DataJpaTest:** PersistenciaJpaTest, ConsultasAvanzadasJpaTest y UsuarioJpaTest. H2, ORM real y rollback por prueba.
- **@SpringBootTest:** EcommerceApplicationTests, CasosDeUsoJpaIntegrationTest, FlujoCompraIntegrationTest, CompraHttpIntegrationTest y SeguridadJwtIntegrationTest.
- **@WebMvcTest con Mockito:** PrendaControllerTest, BolsaDeEstiloControllerTest y ReporteControllerTest. Sustituyen casos de uso; no bastan para demostrar JPA real ni el requisito never del seguimiento.
- **Otras pruebas unitarias:** siete clases de dominio, ApiUseCasesTest, ConsultasAvanzadasUseCasesTest y JwtConfigTest.
- No se encontró ninguna llamada `never()` en las 21 clases originales. Los antiguos verifyNoInteractions comprueban otros límites y no sustituyen el requisito de no guardar desde un caso real.
- FlujoCompraIntegrationTest usa un spy de repositorio para provocar un fallo posterior a flush. Es válido para su rollback, pero **no se elige como prueba de integración sin dobles** del punto 3.

## 2. Matriz final de cumplimiento

| Punto | Estado técnico | Evidencia principal | Decisión |
| --- | --- | --- | --- |
| 0 Repositorio, dos pruebas mínimas | Cumplido | Guardado/relectura y ranking en H2 | Reutilizar pruebas existentes sin duplicarlas |
| 1 Misma ruta 401/403/200 | Cumplido | Tres métodos de BolsaSeguridadSeguimientoIntegrationTest | Añadir tres casos con una bolsa común y tokens de login reales |
| 2 @DataJpaTest y comparación | Cumplido | PersistenciaJpaTest y explicación en README | No agregar dependencias existentes |
| 3 Integración completa | Cumplido | CompraHttpIntegrationTest y ejecución aislada | Reutilizar controller/caso/JPA/H2 reales, sin mocks/spies |
| 4 Dos errores y never guardar | Cumplido | Dos métodos de CompraControllerSeguimientoTest | Importar caso real y simular sus repositorios participantes |

El cierre técnico no implica que ya se haya publicado el código o entregado Classroom. Los pendientes de publicación, acceso y captura visual se detallan al final.

## 3. Punto 0 y punto 2 Repositorios reales con H2

### 0.1 Guardar y recuperar PrendaDeAutor

**Archivo:** `src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/PersistenciaJpaTest.java`.
**Clase/método:** `PersistenciaJpaTest.guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado`.

Arrange usa una camisa conocida: nombre Camisa de lino, descripción Hecha a mano, Dinero de 150000.1234500 COP, dos variantes CAM-M/M/5 y CAM-L/L/3, stockTotal 8 y venta suspendida. Los UUID se generan para aislamiento y se conservan en el objeto original, que es el esperado explícito al releer.
Act llama guardarPrenda, hace EntityManager.flush() y clear() y vuelve a consultar por el ID original. Así no confunde la entidad ya gestionada en caché con una relectura.
Assert compara ID, creador, nombre, descripción, Dinero completo (monto, escala y moneda), stock total, estado y número de variantes. Para cada variante comprueba ID, SKU, talla y stock; un COUNT SQL confirma las dos filas hijas. No se reduce a assertNotNull.

### 0.2 Ranking de variedad de creadores

**Archivo:** `src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java`.
**Método principal:** `rankingCuentaVariantesYPrendasDistintasCoincideConSqlReal`.

@BeforeEach inserta tres prendas controladas: creador A con Camisa y Vestido, dos variantes cada una; creador B con Chaqueta y una variante. Se hace flush/clear. Los creadores son UUID externos, como en el modelo: no se inventa una tabla creador inexistente.
La consulta del adaptador ejecuta JPQL real en H2. Se espera exactamente la lista ordenada [(A,4,2),(B,1,1)], lo que comprueba cardinalidad, asociación, conteos y orden. Además se contrasta con SQL independiente JOIN/GROUP BY/COUNT/COUNT DISTINCT.

Filtros y bordes reutilizados de la misma clase:

| Método | Qué comprueba |
| --- | --- |
| excluyePrendasEliminadasDelContenidoConteoYRanking | Una raíz soft-deleted deja de contribuir al ranking; fila marcada se comprueba con SQL |
| excluyeVarianteHuerfanaEliminadaSinPerderPrenda | Excluir una variante eliminada sin perder la prenda vigente |
| suspensionExcluyeOfertaDelRankingPeroNoDelListado | Excluir oferta suspendida del ranking, conservándola en catálogo |
| desempataRankingPorCreadorAscendente | Orden estable en empates, por UUID de creador |

El ranking mide variedad, no ventas, e incluye variantes con stock cero. Las pruebas usan fixtures propios y rollback, no el contenido variable del seed de desarrollo.

### Comparación aplicada a BOHEME

`@DataJpaTest` carga el slice de persistencia, entidades, EntityManager, repositorios Spring Data y base de pruebas. Los adaptadores JpaPrendaDeAutorRepository/JpaBolsaDeEstiloRepository y los mappers propios se añaden con @Import; no son beans generados automáticamente de Spring Data. El slice no carga todos los controllers, filtros o CommandLineRunner de seed.
`@SpringBootTest` carga el contexto completo de BOHEME. Con @AutoConfigureMockMvc permite integrar filtros JWT, controllers, casos, adaptadores y H2. MockMvc invoca la pila web dentro del proceso de pruebas: no afirma abrir un puerto TCP ni usar la instancia de demostración.

En Spring Boot 4.1.1 el import real es:

~~~java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
~~~

El starter ya estaba en [build.gradle](../../build.gradle):

~~~groovy
testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
~~~

H2 ya era dependencia runtime y las pruebas MVC tienen spring-boot-starter-webmvc-test. Para Mockito se usa `org.springframework.test.context.bean.override.mockito.MockitoBean`. No se añadieron versiones ni dependencias incompatibles. Se evitó el antiguo paquete de DataJpaTest de Boot 3.

## 4. Punto 1 Tres escenarios JWT con un recurso común

**Archivo:** [BolsaSeguridadSeguimientoIntegrationTest.java](../../src/test/java/com/uniquindio/ecommerce/infrastructure/security/BolsaSeguridadSeguimientoIntegrationTest.java).
**Contexto:** @SpringBootTest + @AutoConfigureMockMvc, sin @MockitoBean, sin spy y sin WithMockUser.

| Método exacto | Petición y resultado | Significado |
| --- | --- | --- |
| consultarMismaBolsaSinTokenDevuelve401 | GET de rutaActiva sin Authorization → 401 NO_AUTENTICADO | No hay autenticación |
| consultarMismaBolsaConJwtVendedorValidoDevuelve403 | El mismo GET y JWT VENDEDOR válido → 403 ACCESO_DENEGADO | La firma no concede rol COMPRADOR |
| consultarMismaBolsaConJwtCompradorPropietarioDevuelve200 | El mismo GET y JWT COMPRADOR dueño → 200 y bolsa exacta | Autenticación, rol y propiedad correctos |

La preparación común registra comprador/vendedor mediante la API real, inicia sesión y crea una sola bolsa del comprador. rutaActiva se calcula una sola vez con su UUID; todos los métodos usan esa misma cadena y ningún caso cambia datos ni SecurityConfig.
`autenticarYVerificarJwt` llama al JwtDecoder real: comprueba firma y validadores de producción; añade aserciones de HS256, iss, sub, correo, rol, iat, exp y tokenType. BCrypt y persistencia de usuarios también son reales.
El 200 comprueba bolsaId, clienteId, cerrada=false y líneas vacías. Cada caso consulta H2 para confirmar que hay una sola bolsa, con el mismo dueño, abierta y sin líneas. La clase tiene exactamente tres @Test.

## 5. Punto 3 Integración completa reutilizada

**Archivo:** [CompraHttpIntegrationTest.java](../../src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraHttpIntegrationTest.java).
**Método:** `flujoCompletoConErrorIntencionalStockPedidoYRelecturaDesdeH2`.

Una prueba de integración verifica componentes conectados, no sólo una función con colaboradores simulados. Aquí se registra/inicia sesión, se publica una prenda de 19.95 COP y stock 3, se crea la bolsa y se intenta agregar 4 unidades (409). Luego agrega 2, consulta subtotal, confirma (201) y consulta el histórico (200). Los campos monetarios extra del comprador no deciden el precio de catálogo.

Recorrido efectivo:

~~~text
MockMvc y filtros JWT
  → PrendaController / BolsaDeEstiloController / CompraController reales
  → casos de uso y reglas de dominio reales
  → contratos y adaptadores JPA reales
  → Spring Data / Hibernate
  → H2 compra_http
  → JdbcTemplate consulta por pedidoId y varianteId
~~~

Se comprueba con SQL una fila del pedido y stock restante 1; los GET posteriores verifican cantidades, estado PENDIENTE y total 39.90 COP. No hay mocks ni spies en esta clase. Detecta fallos de wiring, autorización, mapeo y transacción que una prueba unitaria aislada no cubre. Se utiliza para recorridos críticos; una unitaria resulta más acotada para una regla o delegación.

**Aislamiento:** la clase usa una H2 propia, seed desactivado, correos UUID y comprador nuevo por método. Los conteos globales se comparan con la línea base del propio método y las comprobaciones de filas se filtran por IDs recién creados. No hay @Order ni dependencia de métodos anteriores. Se ejecutó además este método de forma aislada; su resultado se conserva en [gradle-integracion-aislada.log](gradle-integracion-aislada.log) y [integracion-aislada.json](integracion-aislada.json).

## 6. Punto 4 Controller y caso real con Mockito never

**Archivo:** [CompraControllerSeguimientoTest.java](../../src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraControllerSeguimientoTest.java).
**Ruta de ambos escenarios:** POST `/api/bolsas/{bolsaId}/items`, JSON válido con varianteId y cantidad 2.

| Método exacto | Preparación | Aserciones |
| --- | --- | --- |
| agregarABolsaInexistenteDevuelve404SinSolicitarGuardado | BolsaDeEstiloRepository.obtenerBolsaParaActualizar devuelve Optional.empty | 404, RECURSO_NO_ENCONTRADO, mensaje La bolsa no existe., path y never guardar |
| agregarCantidadSuperiorAlStockDevuelve409SinSolicitarGuardado | Bolsa real propia abierta y PrendaDeAutor real con una unidad; mocks resuelven datos | 409, STOCK_INSUFICIENTE, mensaje del dominio, path, bolsa sin líneas y stock 1; never guardar |

@WebMvcTest conserva controller, mappers, handler, configuración JSON y SecurityConfig. Se importa `AgregarVarianteABolsaUseCase` real. Sus puertos BolsaDeEstiloRepository, PrendaDeAutorRepository, CatalogoVariantesRepository y UsuarioActual se sustituyen con @MockitoBean. Sólo los casos de otras rutas, Confirmar/Consultar, se simulan como dependencias no ejercitadas del controller.

El 404 pasa por el caso real y la consulta a bolsas. El 409 llega realmente a BolsaDeEstilo.agregarLinea y su comprobación de stock; no se programa un mock de caso de uso para lanzar la excepción.

~~~java
verify(bolsas).obtenerBolsaParaActualizar(id);
verify(bolsas, never()).guardarBolsa(any(BolsaDeEstilo.class));
verify(prendas, never()).guardarPrenda(any(PrendaDeAutor.class));
~~~

La comprobación principal de no guardar es la de **bolsas**, el mismo mock consultado por el caso y donde se guardaría una adición válida. El no guardado de prendas es una comprobación complementaria; no se usa para fingir participación de un repositorio que no se consultó en el 404.
WithMockUser delimita autenticación en este slice MVC y no pretende probar JWT. No desactiva filtros globalmente ni cambia producción. El punto 1, separado, sí prueba la firma/token/cuenta reales. Esta separación permite observar el repositorio con Mockito sin llamar integración real a un test con dobles.

## 7. AAA, independencia y protección de datos

Todas las pruebas seleccionadas y nuevas expresan Arrange, Act y Assert. Las antiguas de flujo tienen varias acciones con aserciones intercaladas; no se reescriben ni duplican. Los métodos nuevos separan explícitamente sus tres fases.

| Contexto | Base / aislamiento | Prevención de interferencia |
| --- | --- | --- |
| DataJpaTest reutilizados | H2 de pruebas y transacción por test | Slice sin runners seed; fixtures propios; rollback y flush/clear |
| CompraHttpIntegrationTest | jdbc:h2:mem:compra_http | Seed false, usuarios/recursos propios, consultas por IDs |
| BolsaSeguridadSeguimientoIntegrationTest | jdbc:h2:mem:seguimiento_seguridad | Seed y cuentas demo false; única bolsa común de sólo lectura; DirtiesContext cierra al terminar |
| CompraControllerSeguimientoTest | No accede a base | Repositorios Mockito reiniciados por Spring; agregados nuevos por caso |

La propiedad global de pruebas boheme.datos-iniciales.enabled=true se usa por CasosDeUsoJpaIntegrationTest para verificar seed. No se cambió: los contextos nuevos/completos seleccionados la sobrescriben y los slices no cargan el runner. No se modifica ecommercedb ni se usan credenciales de la instancia de demostración.
No se añaden @Disabled, esperas arbitrarias, servicios externos, cambios de reglas ni nuevos endpoints. Ninguna de las 21 clases anteriores fue modificada. Las cinco pruebas nuevas se justifican por las brechas de los puntos 1 y 4, no por una meta de cantidad.

## 8. Ejecución y evidencias comprobables

Desde la raíz, con JDK 21 y JAVA_HOME/PATH correctos (ver [README principal](../../README.md#instalación-y-ejecución)):

~~~powershell
.\gradlew.bat clean test
~~~

Las pruebas tienen su configuración ficticia propia en src/test/resources/application.properties. No se necesita iniciar Spring Boot aparte ni definir un secreto de producción. No publicar JWT emitidos durante tests.

Para comprobar sólo los cinco escenarios añadidos:

~~~powershell
.\gradlew.bat test --tests '*BolsaSeguridadSeguimientoIntegrationTest' --tests '*CompraControllerSeguimientoTest'
~~~

Para comprobar la independencia de la integración reutilizada:

~~~powershell
.\gradlew.bat test --tests 'com.uniquindio.ecommerce.infrastructure.rest.controller.CompraHttpIntegrationTest.flujoCompletoConErrorIntencionalStockPedidoYRelecturaDesdeH2'
~~~

Las corridas filtradas regeneran el reporte parcial: ejecutar clean test completo antes de tomar la captura final. No sumar esas corridas como pruebas adicionales de la suite.

**Resultado final:** consultar [resultado-final.json](resultado-final.json), generado desde los XML nuevos, y [gradle-clean-test.log](gradle-clean-test.log). La suite completa conserva 219 ejecuciones anteriores y añade 5: **224 exitosas en 23 clases; 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESSFUL**.
El resumen incluye los métodos seleccionados y su estado individual; no se basa sólo en la última línea de Gradle. Los avisos de API obsoleta e instrumentación JVM no son fallos de tests.

Reportes:

- Vivo, después de ejecutar: `build/reports/tests/test/index.html`.
- XML: `build/test-results/test/TEST-*.xml`.
- Copia completa del HTML final: [reporte-gradle-html.zip](reporte-gradle-html.zip), para conservar el verde aunque otra corrida reemplace build.
- Resultados iniciales: [auditoria-inicial.json](auditoria-inicial.json). Las evidencias previas de docs/evidencias-2.6 a 2.9 permanecen intactas.

### Captura real del verde

La captura debe proceder del reporte generado o de la terminal real, nunca de una imagen dibujada con cifras escritas manualmente. El estado final de captura se registra en [captura-estado.json](captura-estado.json).
Si la automatización no puede capturar el navegador, hacerlo manualmente:

1. Terminar clean test y abrir build/reports/tests/test/index.html (o extraer la copia ZIP y abrir index.html).
2. Dejar visible el resumen de 224 tests, cero fallos/omitidas, éxito 100%, y las clases nuevas de 3 y 2 tests en verde.
3. Tomar un recorte real con Win+Shift+S o la herramienta Recortes de Windows; guardar como captura-gradle-verde.png en esta carpeta.
4. Si se prefiere terminal, mostrar BUILD SUCCESSFUL de la ejecución completa. El log texto no reemplaza por sí solo la captura pedida.
5. No presentar como captura los ejemplos del README ni un resultado generado artificialmente.

## 9. README y archivos propios del seguimiento

Se añadió exactamente `## Pruebas avanzadas` al final del [README raíz](../../README.md#pruebas-avanzadas), preservando su contenido anterior. Los puntos 1, 2 y 3 tienen **seis líneas explicativas cada uno**, con saltos Markdown, y referencias a clases/métodos. Los puntos 0 y 4 tienen referencias breves. La cifra 219 del Entregable 2 conserva su contexto histórico; el resultado de esta actividad está en esta carpeta.

Archivos nuevos de código:

- `src/test/java/com/uniquindio/ecommerce/infrastructure/security/BolsaSeguridadSeguimientoIntegrationTest.java`: tres casos del punto 1.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/CompraControllerSeguimientoTest.java`: dos casos del punto 4.

No se modificó ninguna prueba existente, build.gradle, configuración, producción ni los tres documentos finales del Entregable 2. No se hizo commit ni push.

## 10. Preparación de la entrega en Classroom

| Elemento | Estado / acción manual |
| --- | --- |
| Código y README | Preparados localmente; revisar cambios y autorizar su publicación antes de compartir enlace |
| Repositorio | Remoto configurado https://github.com/sofism01/ecommerce-PA; no confundir URL existente con pruebas nuevas ya publicadas |
| Captura verde | Revisar captura-estado.json; si está pendiente, seguir los cinco pasos anteriores |
| Classroom | Adjuntar enlace accesible al repositorio, captura real y referencia a la sección Pruebas avanzadas |
| Permisos | Comprobar acceso de la docente al repositorio y a los adjuntos |
| PDF original | Cotejo literal pendiente si se requiere; se implementaron los puntos 0–4 transcritos por el usuario |

**Conclusión:** los cinco puntos técnicos transcritos están implementados y respaldados por pruebas ejecutadas. La entrega académica sólo queda completa cuando se publique con autorización, se adjunte la captura real requerida y se entregue Classroom. No se declara realizada ninguna de esas acciones externas.
