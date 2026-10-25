# BOHEME — Entregable 2, apartado 2.8: pruebas automatizadas

## Conclusión

**El apartado 2.8 cumple completamente la rúbrica indicada:** se reutilizan ocho
pruebas concretas del dominio, cuatro de casos de uso con repositorios en memoria,
una de controller con `@WebMvcTest` y una de repositorio con H2, sin duplicarlas.
La suite completa conserva **189 ejecuciones en 18 clases**.

Se revisaron todas las clases de `src/test/java`. No se crearon pruebas nuevas.
El único ajuste fue hacer explícitos AAA y la aserción de contexto activo en el
método `EcommerceApplicationTests.contextLoads()`, que antes tenía el cuerpo vacío
y comprobaba implícitamente el arranque mediante `@SpringBootTest`.

Esta auditoría corresponde exclusivamente a 2.8. No se modificaron funcionalidades
de producción, JWT, JPA, endpoints ni reglas del dominio. No se trabajó en 2.9 ni
en ninguna otra actividad académica. No se hicieron commits ni push.

## Resultado real de Gradle

Comando ejecutado con Java 21:

```powershell
.\gradlew.bat clean test --console=plain
```

`--console=plain` sólo facilita conservar el log; se ejecutaron las tareas solicitadas
`clean test`, con recompilación de producción y pruebas, sin seleccionar un subconjunto.

| Resultado | Cantidad / estado |
|---|---:|
| Clases ejecutadas | 18 |
| Pruebas ejecutadas | **189** |
| Pruebas exitosas | **189** |
| Fallidas | **0** |
| Errores | **0** |
| Omitidas | **0** |
| Resultado final | **BUILD SUCCESSFUL** |
| Código de salida | **0** |

El conteo corresponde a ejecuciones JUnit, incluidas las invocaciones de pruebas
parametrizadas. No se cuentan aserciones como pruebas independientes.

Evidencia: [log completo](evidencias-2.8/gradle-clean-test.log),
[resumen por clase y por prueba](evidencias-2.8/resumen-pruebas.json),
[informe HTML final comprimido](evidencias-2.8/reportes-2.8-final-html.zip) y
[XML finales](evidencias-2.8/reportes-2.8-final-xml.zip).
El informe navegable generado por Gradle permanece en `build/reports/tests/test/index.html`.

## Clasificación de las 189 pruebas

La clasificación es por el objetivo de cada prueba, no únicamente por su carpeta.
Las categorías son excluyentes y suman 189:

| Categoría | Ejecuciones | Alcance |
|---|---:|---|
| Dominio | **42** | 36 en los paquetes domain, 5 validaciones de ConsultaPrendas y 1 rechazo de contraseña plana en Usuario |
| Casos de uso unitarios | **6** | 4 con repositorios en memoria y 2 con Mockito |
| Controllers | **57** | 45 de prendas, 10 de bolsas y 2 de reportes mediante WebMvcTest |
| Repositorios | **34** | 12 de persistencia, 19 de consultas JPA y 3 de usuarios, con DataJpaTest/H2 |
| Otras existentes | **50** | 4 de integración de casos de uso/JPA, 37 de seguridad, 8 de configuración JWT y 1 de arranque |
| **Total** | **189** | Sin doble conteo |

En particular, las cinco invocaciones de
`ConsultasAvanzadasUseCasesTest.criteriosRechazanPaginacionInvalidaSinSpring(...)`
comprueban un tipo del dominio directamente; **no** se presentan como cinco casos
de uso. Del mismo modo, `JwtConfigTest.dominioNoAceptaPasswordPlanoComoHash()`
se clasifica como dominio y las otras ocho invocaciones como configuración.

Inventario de todas las clases, con el mismo prefijo
`src/test/java/com/uniquindio/ecommerce/`:

| Archivo | Ejecuciones | Clasificación |
|---|---:|---|
| `domain/entity/BolsaDeEstiloTest.java` | 2 | Dominio |
| `domain/entity/EntidadTest.java` | 3 | Dominio |
| `domain/entity/InvariantesEntidadesTest.java` | 21 | Dominio |
| `domain/entity/PrendaDeAutorTest.java` | 2 | Dominio |
| `domain/entity/ReconstruccionAgregadosTest.java` | 4 | Dominio |
| `domain/valueobject/ValueObjectTest.java` | 4 | Dominio |
| `application/usecase/ApiUseCasesTest.java` | 4 | Casos de uso en memoria |
| `application/usecase/ConsultasAvanzadasUseCasesTest.java` | 7 | 2 casos de uso con Mockito + 5 de dominio |
| `infrastructure/rest/controller/PrendaControllerTest.java` | 45 | Controller |
| `infrastructure/rest/controller/BolsaDeEstiloControllerTest.java` | 10 | Controller |
| `infrastructure/rest/controller/ReporteControllerTest.java` | 2 | Controller |
| `infrastructure/persistence/jpa/PersistenciaJpaTest.java` | 12 | Repositorio H2 |
| `infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java` | 19 | Repositorio H2 |
| `infrastructure/persistence/jpa/UsuarioJpaTest.java` | 3 | Repositorio H2 |
| `infrastructure/persistence/jpa/CasosDeUsoJpaIntegrationTest.java` | 4 | Integración, separada de unitarias |
| `infrastructure/security/SeguridadJwtIntegrationTest.java` | 37 | Integración de seguridad existente |
| `infrastructure/security/JwtConfigTest.java` | 9 | 8 de configuración + 1 de dominio |
| `EcommerceApplicationTests.java` | 1 | Arranque del contexto |

## Tabla de cumplimiento de los cuatro mínimos

| Exigencia de 2.8 | Evidencia concreta reutilizada | Resultado |
|---|---|---|
| 8 pruebas del dominio de Entrega 1 | Las ocho identificadas en la siguiente tabla, en clases originales del repositorio | **8/8 pasan**; existen más pruebas de dominio |
| 4 pruebas de casos de uso con repositorios simulados/en memoria, incluidos errores de negocio | Los cuatro métodos de ApiUseCasesTest | **4/4 pasan**, incluido vaciado de bolsa cerrada |
| 1 prueba de controller con WebMvcTest | PrendaControllerTest.registrar_Valido_Devuelve201YDelegaConDominioMapeado | **Pasa** con filtros activos y rol de vendedor |
| 1 prueba de repositorio | PersistenciaJpaTest.guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado | **Pasa** con DataJpaTest y H2 real |
| **Mínimo total** | **14 pruebas diferentes ya existentes** | **Cumplido** |

Las cuatro clases de dominio seleccionadas ya aparecen en la versión base del
repositorio (`git ls-tree HEAD src/test/java`); no son pruebas creadas para esta
auditoría. Se verificó su implementación actual, adaptada en apartados anteriores
a las fábricas e invariantes del dominio.

### Ocho pruebas concretas de dominio reutilizadas

| Archivo dentro de domain | Método | Resultado que comprueba |
|---|---|---|
| `entity/PrendaDeAutorTest.java` | `crearPrenda_SinVariantes_LanzaExcepcion` | Una prenda sin variantes es rechazada con ReglaDominioException y el mensaje esperado |
| `entity/PrendaDeAutorTest.java` | `actualizarPrecio_Valido_CambiaPrecio` | El precio válido queda en la prenda resultante |
| `entity/BolsaDeEstiloTest.java` | `agregarLinea_StockInsuficiente_NoCambiaEstado` | Stock insuficiente produce la excepción esperada y deja la bolsa vacía |
| `entity/BolsaDeEstiloTest.java` | `modificarBolsaCerrada_LanzaExcepcion_NoCambiaEstado` | La bolsa cerrada rechaza la modificación sin alterar su contenido |
| `valueobject/ValueObjectTest.java` | `igualdadPorValor_Dinero` | Dos importes iguales tienen igualdad por valor |
| `valueobject/ValueObjectTest.java` | `validacionMontoNegativo_Dinero` | El importe negativo es rechazado con el error esperado |
| `valueobject/ValueObjectTest.java` | `igualdadPorValor_SKU` | Dos SKU iguales tienen igualdad por valor |
| `valueobject/ValueObjectTest.java` | `validacionSKUVacio` | SKU vacío/blanco es rechazado con el error esperado |

### Cuatro pruebas concretas de casos de uso reutilizadas

Todas están en `application/usecase/ApiUseCasesTest.java` y construyen los
repositorios `InMemoryPrendaDeAutorRepository` o `InMemoryBolsaDeEstiloRepository`
directamente. No requieren Spring ni H2:

| Método | Caso y aserciones relevantes |
|---|---|
| `listado_RecuperaTodasLasPrendasYFiltraPorCreador` | Registra prendas de dos creadores; comprueba cantidad y prendas exactas del creador, además del resultado vacío para otro UUID |
| `suspensionYStock_SeConservanEnElRepositorio` | Actualiza stock de una variante y suspende venta; relee y comprueba stock cero y venta suspendida |
| `prendasYVariantesAusentes_SeDiferencianDeOtrasReglas` | Invoca casos de uso con prenda/variante ausentes y comprueba RecursoNoEncontradoException |
| `bolsa_FlujoActivoVaciadoYCierreYErroresConservanReglas` | Crea, vacía, cierra y consulta; comprueba estado y ausencia de bolsa activa. Vaciar una bolsa cerrada debe lanzar ModificacionBolsaNoPermitidaException; las bolsas inexistentes también se rechazan |

La última prueba incluye una **regla de negocio real**; no se utiliza únicamente
un mock configurado para lanzar una excepción. Se ejecuta la implementación del
caso de uso y el dominio sobre el repositorio en memoria.

## Revisión de calidad y separación de responsabilidades

- **AAA:** las pruebas representativas preparan datos/repositorios, ejecutan el
  comportamiento y comprueban valores, estado, errores o delegación. `Act & Assert`
  es apropiado en `assertThrows` y en cadenas MockMvc `perform(...).andExpect(...)`.
  Los fixtures `BeforeEach`/`BeforeAll` y helpers son parte de Arrange; no se
  duplicaron para agregar comentarios artificiales.
- **Nombres:** los métodos identifican la operación y la condición/resultado.
  `contextLoads` conserva su nombre convencional y ahora tiene AAA/aserción explícitos.
- **Invariantes:** los fixtures crean agregados mediante fábricas y Value Objects;
  las entradas inválidas se usan deliberadamente dentro de las aserciones de rechazo.
  No se alteró ninguna regla del dominio para hacer pasar una prueba.
- **Unitarias:** dominio, casos de uso en memoria/Mockito y configuración se
  distinguen de los tests de persistencia e integración. Los cuatro casos de uso
  en memoria justifican el mínimo sin contar integración como prueba unitaria.
- **Controllers en Boot 4:** se utiliza
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, MockMvc,
  `MockitoBean`, los mappers reales y el manejador de excepciones.
- **Seguridad en los slices:** se conserva SecurityConfig y `WithMockUser` con
  VENDEDOR/COMPRADOR donde corresponde, junto al starter de pruebas de seguridad
  ya instalado. No se deshabilitaron filtros ni se excluyó la seguridad para obtener verde.
  La seguridad con JWT real continúa cubierta por su suite existente.
- **Repositorio real:** se utiliza
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`. El test
  seleccionado hace `flush()` y `clear()` antes de releer: no basta con comprobar
  la instancia del contexto de persistencia. Verifica los atributos, variantes,
  estado y la cantidad de filas mediante SQL.
- **H2 comprobado:** el XML de PersistenciaJpaTest registra la sustitución del
  datasource por una base embebida y el arranque de una URL `jdbc:h2:mem:...`.
  [Evidencia de arquitectura y H2](evidencias-2.8/arquitectura-y-h2.json).

## Dominio independiente de infraestructura

Se inspeccionaron los archivos Java de `src/main/java/com/uniquindio/ecommerce/domain`.
Sus imports pertenecen al JDK o al propio dominio. No se encontraron imports ni
referencias a Spring, Spring Security, JPA, Hibernate, Servlet ni clientes HTTP.
No se añadieron dependencias al dominio durante esta tarea.

La comprobación SHA-256 de los 129 archivos de `src` identifica únicamente el
ajuste de `EcommerceApplicationTests.java`. El código de producción y las demás
pruebas permanecen intactos:
[inventario inicial](evidencias-2.8/fuentes-antes.json),
[comparación final](evidencias-2.8/verificacion-fuentes.json).

## Cinco pruebas recomendadas para el video

Rutas relativas a `src/test/java/com/uniquindio/ecommerce/`:

| Demostración | Archivo y método | Qué mostrar y por qué es relevante |
|---|---|---|
| Dominio | `domain/entity/PrendaDeAutorTest.java:20` — `crearPrenda_SinVariantes_LanzaExcepcion` | Arrange con ID, creador y precio; intento de crear sin variantes; assertThrows y mensaje. Demuestra que la invariante se protege en el dominio |
| Caso de uso del vendedor | `application/usecase/ApiUseCasesTest.java:42` — `suspensionYStock_SeConservanEnElRepositorio` | Repositorio en memoria, actualización de stock y suspensión, relectura y dos aserciones. Demuestra el flujo de aplicación sin HTTP/JPA |
| Caso de uso del comprador | `application/usecase/ApiUseCasesTest.java:70` — `bolsa_FlujoActivoVaciadoYCierreYErroresConservanReglas` | Flujo de bolsa, estado cerrado y rechazo del vaciado posterior. Demuestra éxito y regla de negocio incumplida |
| Controller | `infrastructure/rest/controller/PrendaControllerTest.java:101` — `registrar_Valido_Devuelve201YDelegaConDominioMapeado` | WebMvcTest, usuario vendedor, respuesta 201, JSON completo y ArgumentCaptor. Demuestra validación del contrato REST y delegación al caso de uso |
| Repositorio | `infrastructure/persistence/jpa/PersistenciaJpaTest.java:55` — `guardarYRecuperarPrenda_ConservaTodosLosAtributosVariantesYEstado` | DataJpaTest, H2, persistencia, flush/clear, relectura y SQL. Demuestra que la persistencia no es una lista en memoria |

Las cinco ya existían y se ejecutaron como parte del `clean test`. Para el video
puede mostrarse el informe real de Gradle con el resultado de sus clases y luego
la implementación. No se generaron capturas ni resultados artificiales.

## Cambios y conservación de evidencias

**Pruebas nuevas: ninguna.** No se eliminó ni deshabilitó ninguna prueba.

**Único archivo existente modificado:**
`src/test/java/com/uniquindio/ecommerce/EcommerceApplicationTests.java`.
Se inyecta ConfigurableApplicationContext y se comprueba `isActive()` con AAA.
El número de pruebas sigue siendo 189.

**Archivos nuevos:** este informe y los resultados de auditoría en
`docs/evidencias-2.8/`: log completo, resumen JSON de las 189 ejecuciones,
verificación de fuentes, inventario inicial, evidencia H2/arquitectura,
XML e informes HTML comprimidos.

Los resultados anteriores al `clean` no se perdieron:

- [HTML anterior completo: 42 archivos](evidencias-2.8/reportes-previos-html-completo.zip).
- [XML anteriores: 18 suites](evidencias-2.8/reportes-previos-xml.zip), también disponibles en `evidencias-2.8/previos/xml/`.
- Se conservan intactas las evidencias históricas de 2.6 y 2.7.
- Se guardó el log de la primera auditoría con 189/189 antes del ajuste del smoke
  test en `evidencias-2.8/gradle-clean-test-auditoria-inicial.log`.
- Los archivos `reportes-2.8-html.zip` y `reportes-2.8-xml.zip` conservan esa primera
  ejecución; los que contienen `final` corresponden al código final de esta tarea.

La copia inicial encontró un binario temporal bloqueado y rutas largas de Windows.
La preservación se completó mediante ZIP del HTML original y copia de los XML;
no se utilizó la caché binaria de Gradle como evidencia. No quedó pendiente la
conservación de ningún informe HTML/XML solicitado.

## Problemas pendientes y alcance final

**No hay problemas pendientes que impidan cumplir 2.8.** Gradle emitió avisos de
API obsoleta/instrumentación de JVM, sin fallos de compilación o pruebas.

Esta auditoría verifica ejecución, aserciones, AAA, separación de tipos de prueba,
H2 y los cuatro mínimos indicados. No afirma cobertura porcentual ni resultados
de verificaciones que no se ejecutaron. No incluye ninguna actividad adicional.

**Conclusión expresa: el apartado 2.8 cumple completamente la rúbrica de la profesora.**
