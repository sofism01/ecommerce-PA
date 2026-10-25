# BOHEME — Entregable 2, apartado 2.6: consultas avanzadas

## Resultado y alcance

Implementado y comprobado el 24 de octubre de 2026 con Java 21 y Spring Boot 4.1.1.
Se conservan las entidades, invariantes, seed y ocho operaciones anteriores.
Se añaden dos consultas de lectura con este flujo:

`REST → caso de uso → ConsultaPrendasRepository (dominio) → JpaPrendaDeAutorRepository → H2`.

No se implementó el apartado 2.7, autenticación, frontend, pedidos, pagos ni MariaDB.
No se hicieron commits ni push. El repositorio ya tenía cambios previos sin commit:
el inventario siguiente identifica exclusivamente los archivos tocados por esta tarea,
independientemente de que Git ya los mostrara como nuevos o modificados.

## Archivos creados en esta implementación

Rutas Java relativas a `src/main/java/com/uniquindio/ecommerce/`:

| Archivo | Responsabilidad |
|---|---|
| `domain/query/ConsultaPrendas.java` | Criterios, normalización de blancos y validación de paginación/moneda |
| `domain/query/ConsultaInvalidaException.java` | Error específico de consulta inválida |
| `domain/query/Pagina.java` | Resultado paginado propio, contenido copiado a lista inmutable |
| `domain/query/VariedadCreador.java` | Resultado agregado propio |
| `domain/repository/ConsultaPrendasRepository.java` | Puerto de lectura sin Spring/JPA |
| `application/usecase/BuscarPrendasUseCase.java` | Caso de uso de listado paginado |
| `application/usecase/ConsultarVariedadCreadoresUseCase.java` | Caso de uso de ranking |
| `application/dto/response/PaginaPrendasResponse.java` | Contrato REST paginado |
| `application/dto/response/VariedadCreadorResponse.java` | Contrato REST del ranking |
| `infrastructure/rest/mapper/ConsultaPrendasMapper.java` | Conversión de resultados a DTO REST |
| `infrastructure/rest/controller/ReporteController.java` | Endpoint del reporte y OpenAPI |

Otros archivos creados:

- `src/test/java/com/uniquindio/ecommerce/application/usecase/ConsultasAvanzadasUseCasesTest.java`.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/ConsultasAvanzadasJpaTest.java`.
- `src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/ReporteControllerTest.java`.
- `scripts/verificar-consultas-avanzadas.py`: comprobación reproducible HTTP y SQL mediante la consola H2.
- Este informe.
- `docs/evidencias-2.6/verificacion.json`: peticiones, estados, cuerpos JSON, SQL y filas reales.
- `docs/evidencias-2.6/pruebas.json`: resumen de las suites extraído de los XML de JUnit.
- `docs/evidencias-2.6/catalogo-h2.html`, `ranking-h2.html`, `pagina-h2.html`: respuestas originales de la consola H2.

## Archivos existentes modificados

| Archivo relativo a la raíz | Cambio |
|---|---|
| `src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/repository/PrendaJpaRepository.java` | JPQL paginada, countQuery, fetch por IDs y ranking |
| `src/main/java/com/uniquindio/ecommerce/infrastructure/persistence/jpa/JpaPrendaDeAutorRepository.java` | Implementa también el puerto de lectura; adapta Page a Pagina |
| `src/main/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaController.java` | GET paginado, dependencias de lectura y OpenAPI |
| `src/main/java/com/uniquindio/ecommerce/infrastructure/rest/exception/ApiExceptionHandler.java` | Consulta inválida → 400 con contrato ErrorResponse existente |
| `src/test/java/com/uniquindio/ecommerce/infrastructure/rest/controller/PrendaControllerTest.java` | Nuevas pruebas; agrega mock y mapper requeridos sin eliminar ni desactivar pruebas anteriores |

No fue necesario modificar `build.gradle`, entidades, datos iniciales ni configuración de H2.
El contrato original `PrendaDeAutorRepository` permanece intacto: el nuevo puerto de
consultas evita imponer consultas avanzadas al adaptador antiguo en memoria.

## Endpoints y semántica

### GET /api/prendas

| Parámetro opcional | Comportamiento |
|---|---|
| `q` | Subcadena en nombre **o** descripción, sin distinguir mayúsculas; elimina espacios externos. Blanco equivale a omitirlo |
| `creadorId` | UUID exacto; formato inválido devuelve 400 |
| `moneda` | Tres letras, sin distinguir mayúsculas; blanco omite filtro. No consulta catálogo ISO: busca el código almacenado |
| `page` | Desde 0, por defecto 0; negativo/no entero/fuera de int → 400 |
| `size` | De 1 a 100, por defecto 20; otro valor → 400 |

Los filtros se combinan con AND; nombre/descripción con OR. `%`, `_` y `!` se
buscan como caracteres literales, no como comodines aportados por el cliente.
Orden fijo: `nombre ASC, id ASC`, según la colación de H2; no se promete búsqueda
sin acentos. El listado incluye prendas suspendidas y excluye eliminadas.
Una página vacía devuelve 200; fuera de rango conserva el total de coincidencias.
Se rechaza un desplazamiento `page * size` superior a `Integer.MAX_VALUE`, límite de JPA.

### GET /api/reportes/creadores/variedad

Ranking de variantes ofertadas por creador, obtenido de prendas y variantes
persistidas. Sólo considera prendas `disponibleParaLaVenta=true` y filas no
eliminadas. Esta elección usa el estado de oferta que ya existe en el modelo:
suspender la venta retira esa prenda del ranking. El listado general sí permite verla.

Incluye variantes de stock cero: mide amplitud de catálogo, no existencias ni ventas.
`cantidadPrendas` cuenta las prendas distintas que aportan variantes vigentes.
El JOIN interno omite creadores sin variantes ofertadas. No existe una entidad JPA
de creador ni se inventan nombres, pedidos, ventas o relaciones con bolsas.
Orden: cantidad de variantes descendente y UUID del creador ascendente para empates.
Sin oferta devuelve 200 con `[]`.

## JPQL real

Los nombres se comprobaron contra las entidades: `nombre`, `descripcion`,
`creadorId`, `precio.moneda`, `disponibleParaLaVenta`, `variantes` e `id`.
Las tres consultas parametrizadas usan `@Query` y parámetros nombrados `@Param`.

Primera consulta, con `Pageable` exclusivamente en infraestructura:

```jpql
select p.id from PrendaJpaEntity p
where (:texto is null or lower(p.nombre) like :texto escape '!'
    or lower(p.descripcion) like :texto escape '!')
  and (:creadorId is null or p.creadorId = :creadorId)
  and (:moneda is null or upper(p.precio.moneda) = :moneda)
order by p.nombre asc, p.id asc
```

`texto` se enlaza como `%subcadena%`, en minúsculas y con los comodines escapados;
`moneda` se enlaza en mayúsculas. Esto prepara parámetros, sin filtrar resultados en Java.

Consulta de conteo explícita, con los mismos predicados y sin JOIN que multiplique filas:

```jpql
select count(p.id) from PrendaJpaEntity p
where (:texto is null or lower(p.nombre) like :texto escape '!'
    or lower(p.descripcion) like :texto escape '!')
  and (:creadorId is null or p.creadorId = :creadorId)
  and (:moneda is null or upper(p.precio.moneda) = :moneda)
```

Segunda consulta, sólo para los IDs devueltos por H2 en esa página:

```jpql
select distinct p from PrendaJpaEntity p
left join fetch p.variantes
where p.id in :ids
order by p.nombre asc, p.id asc
```

La paginación se aplica a IDs sin fetch de colección: H2 ejecuta límite y desplazamiento.
Después se cargan las entidades/variantes de esos IDs en un solo fetch, sin Pageable.
`DISTINCT` evita duplicados del padre y el orden se expresa de nuevo en la base.
Los streams sólo convierten esa página a dominio/DTO; no filtran ni paginan.
Si la página está vacía no se ejecuta el fetch ni un `IN ()`.

La prueba con 13 prendas y página de 5 verificó **tres sentencias**: IDs, COUNT y
fetch. Comprobó `OFFSET` y `FETCH FIRST` en SQL, y activó
`hibernate.query.fail_on_pagination_over_collection_fetch=true`: habría fallado
si Hibernate hubiese tenido que paginar una colección cargada en memoria.
Spring Data puede omitir COUNT cuando deduce el total de una página incompleta;
los metadatos conservan el mismo contrato.

Ranking:

```jpql
select new com.uniquindio.ecommerce.domain.query.VariedadCreador(
    p.creadorId, count(v.id), count(distinct p.id))
from PrendaJpaEntity p join p.variantes v
where p.disponibleParaLaVenta = true
group by p.creadorId
order by count(v.id) desc, p.creadorId asc
```

`JOIN` enlaza cada prenda con sus variantes reales. `GROUP BY` genera una fila
por creador. `COUNT(v.id)` cuenta variantes; `COUNT(DISTINCT p.id)` evita contar
una misma prenda varias veces por tener varias tallas. La proyección devuelve
UUID y dos valores long, sin cargar agregados ni hacer agrupación en Java.

### Eliminación lógica

Se conserva `@SoftDelete(columnName="eliminado", strategy=DELETED)` en ambas
entidades. `eliminado` es la columna gestionada por Hibernate, **no un atributo
JPA accesible en JPQL**. Hibernate agrega `p.eliminado=false` a las consultas de
prendas, incluido COUNT, y `v.eliminado=false` al JOIN de variantes. Se comprobó
con SQL capturado y pruebas de eliminación real, no mediante un campo inventado.
Este tratamiento transparente de entidades asociadas está descrito en la
[documentación de Hibernate](https://docs.hibernate.org/orm/7.1/userguide/html_single/#soft-delete).

## DTO y ejemplos JSON reales

`PaginaPrendasResponse`: `content: PrendaResponse[]`, `totalElements: long`,
`totalPages: int`, `page: int`, `size: int`.
Se reutiliza PrendaResponse con sus variantes, dinero y estado existentes.
`VariedadCreadorResponse`: `creadorId: UUID`, `cantidadVariantes: long`,
`cantidadPrendas: long`.

Respuesta real a `GET /api/prendas?q=LINO&moneda=COP&creadorId=81000000-0000-4000-8000-000000000001`:

```json
{
  "content": [{
    "prendaId": "91000000-0000-4000-8000-000000000001",
    "creadorId": "81000000-0000-4000-8000-000000000001",
    "nombre": "Camisa de lino artesanal",
    "descripcion": "Camisa de autor confeccionada en lino",
    "precioBase": {"monto": 150000.00, "moneda": "COP"},
    "variantes": [
      {"varianteId": "a1000000-0000-4000-8000-000000000001", "sku": "BOH-LINO-M", "tipoTalla": "M", "stockDisponible": 5},
      {"varianteId": "a1000000-0000-4000-8000-000000000002", "sku": "BOH-LINO-L", "tipoTalla": "L", "stockDisponible": 3}
    ],
    "stockTotal": 8,
    "disponibleParaLaVenta": true
  }],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

Respuesta real del ranking inicial:

```json
[
  {"creadorId":"81000000-0000-4000-8000-000000000001","cantidadVariantes":4,"cantidadPrendas":2},
  {"creadorId":"81000000-0000-4000-8000-000000000002","cantidadVariantes":1,"cantidadPrendas":1}
]
```

Sin resultados:

```json
{"content":[],"totalElements":0,"totalPages":0,"page":0,"size":20}
```

Los cuerpos completos de las 34 peticiones están en [verificacion.json](evidencias-2.6/verificacion.json).

## Pruebas ejecutadas

Comando solicitado: `.\gradlew.bat clean test`.
Resultado real: **BUILD SUCCESSFUL; 140 pruebas, 0 fallos, 0 errores, 0 omitidas**.
Se conservan las 99 anteriores y se agregan 41 ejecuciones de pruebas:

| Área | Nuevas ejecuciones |
|---|---:|
| H2 con `@DataJpaTest` | 19 |
| Endpoint paginado con MockMvc | 13 |
| Endpoint del ranking con MockMvc | 2 |
| Casos de uso y validación del contrato | 7 |

Se utiliza el paquete de Spring Boot 4
`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, y la
dependencia de pruebas que ya estaba instalada. Las pruebas H2 hacen flush/clear
antes de consultar y rollback por prueba; usan AAA. Incluyen búsqueda por nombre
y descripción, mayúsculas, los dos filtros y su combinación, varias páginas,
conteos, desempates, caracteres literales, blancos, ausencia de resultados,
página fuera de rango, soft delete de padre y de variante huérfana, suspensión,
ranking vacío y comparación de ranking JPQL con SQL nativo independiente.
MockMvc comprueba 200/400, límites, tipos inválidos, mapeo y delegación.

Resumen permanente: [pruebas.json](evidencias-2.6/pruebas.json).
Informe HTML generado: `build/reports/tests/test/index.html`.
La revisión de imports del paquete domain no encontró Spring, Hibernate ni JPA;
`git diff --check` terminó sin errores.

## Verificación HTTP, Swagger y H2 ejecutada

Se arrancó Spring Boot mediante `bootRun` en **8081**. El arranque inicial en 8080
falló porque ese puerto estaba ocupado; se dejó intacto el proceso existente.
El cambio de puerto es un argumento de ejecución, no un cambio de configuración.

Comprobaciones realizadas mediante HTTP real, además de MockMvc:

- Swagger UI, su inicializador y `/api-docs/swagger-config` responden 200.
- `/api-docs` muestra las **10 operaciones** (8 anteriores y 2 nuevas), seis parámetros del listado y ejemplos de ambas consultas.
- Listado inicial: tres prendas, total 3, una página con tamaño por defecto 20.
- Nombre, descripción y mayúsculas; creador, moneda y combinación de filtros.
- Páginas con tamaño 2: contenidos de 2, 1 y 0; total 3 y totalPages 2.
- Sin coincidencias: 200 y conteos cero; parámetros inválidos: 400 con ErrorResponse.
- Ranking inicial: 4/2 y 1/1, idéntico al SQL ejecutado en la misma H2.
- Ocho operaciones previas: registrar prenda (201), listar por creador (200),
  actualizar stock (204 y relectura), suspender (204 y relectura), crear bolsa
  (201), obtener activa (200), vaciar (204), cerrar (204 y luego activa 404).
- Tras suspender la prenda de verificación, el ranking sigue siendo el inicial.

El script abrió una sesión de la consola H2 del **mismo proceso de Spring** con
`jdbc:h2:mem:ecommercedb;IFEXISTS=TRUE`, `sa`, contraseña vacía. Ejecutó tres
SELECT y guardó las respuestas HTML originales. No usó otra H2 en memoria ni
simuló los resultados.

| Prenda inicial | Moneda | Variantes vigentes |
|---|---|---:|
| Camisa de lino artesanal | COP | 2 |
| Chaqueta de autor | USD | 1 |
| Vestido de algodón | COP | 2 |

SQL de contraste del ranking:

```sql
SELECT p.creador_id, COUNT(v.id) AS cantidad_variantes,
       COUNT(DISTINCT p.id) AS cantidad_prendas
FROM prendas_autor p
JOIN variantes_prenda v ON v.prenda_id=p.id
WHERE p.eliminado=FALSE AND v.eliminado=FALSE
  AND p.disponible_para_venta=TRUE
GROUP BY p.creador_id
ORDER BY COUNT(v.id) DESC, p.creador_id ASC;
```

| Creador | SQL variantes | SQL prendas | REST |
|---|---:|---:|---|
| `81000000-0000-4000-8000-000000000001` | 4 | 2 | Coincide |
| `81000000-0000-4000-8000-000000000002` | 1 | 1 | Coincide |

Evidencia: [catálogo H2](evidencias-2.6/catalogo-h2.html),
[ranking H2](evidencias-2.6/ranking-h2.html), [página H2](evidencias-2.6/pagina-h2.html).

## Tabla de cumplimiento

| Requisito de 2.6 | Implementación | Comprobación real |
|---|---|---|
| Listado paginado con búsqueda y filtro | GET /api/prendas; q, creadorId, moneda | H2 y HTTP |
| JPQL con parámetros nombrados | @Query + @Param; countQuery explícita | Consultas ejecutadas en H2 |
| Contenido, totalElements y totalPages | DTO propio con page y size | H2 y HTTP, varias páginas |
| Exclusión de prendas eliminadas | @SoftDelete en consulta y COUNT | Eliminación real en DataJpaTest |
| JOIN + GROUP BY + COUNT | Ranking de variantes; COUNT DISTINCT de prendas | JPQL comparada con SQL y HTTP |
| Sin negocio quemado en controllers | Casos de uso y puerto de lectura | Revisión y pruebas de delegación |
| Filtrado y paginación en base | IDs con Pageable y predicados JPQL | SQL capturado con OFFSET/FETCH |
| Dominio sin paginación Spring | ConsultaPrendas, Pagina, VariedadCreador propios | Revisión de imports |
| Orden estable y ausencia de duplicados | nombre/ID; DISTINCT; creador en empates | Pruebas H2 específicas |
| Sin N+1 en DTO paginados | Fetch conjunto de variantes por IDs | Tres sentencias para página de cinco |
| Validación de parámetros | Límites, overflow, UUID y moneda | MockMvc y HTTP 400 |
| Sin coincidencias → 200 | Página vacía y conteos correctos | H2, MockMvc y HTTP |
| Variantes eliminadas excluidas | @SoftDelete aplicado al JOIN | Eliminación real de huérfana y SQL |
| Compatibilidad de endpoint por creador | Se conserva ruta y caso de uso | Pruebas anteriores y HTTP |
| Swagger anterior y nuevo | Documentación de dos operaciones nuevas | UI/config/OpenAPI por HTTP: 10 operaciones |
| Conservar 99 pruebas | Sin borrar ni desactivar pruebas previas | clean test: 140/140 |
| Casos de uso de lectura, mappers separados | Dos casos de uso y ConsultaPrendasMapper | Unitarias, MockMvc y aplicación real |

## Reproducción y límites

En este equipo el Java del PATH era Java 8. Para ejecutar se usó el JDK 21 ya instalado:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\boheme-temurin-21.0.12.1\jdk-21.0.12.1+1"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
.\gradlew.bat clean test
.\gradlew.bat bootRun --args=--server.port=8081
```

En otra terminal, sobre una instancia recién iniciada con seed habilitado:

```powershell
python scripts/verificar-consultas-avanzadas.py --base-url http://localhost:8081
```

El script espera exactamente las tres prendas iniciales y crea una muestra de
prenda suspendida y una bolsa cerrada para comprobar las operaciones existentes.
Sus IDs están en `mutacionesDemo` de la evidencia. Para repetirlo debe reiniciarse
la instancia H2; no elimina datos existentes automáticamente.

No hay requisitos de 2.6 pendientes. Límites conservados o propios del alcance:

- H2 es volátil; reiniciar pierde datos. La evidencia guardada corresponde a la ejecución indicada.
- La igualdad y búsqueda textual no eliminan acentos. La moneda se valida por formato, conforme al almacenamiento actual.
- El ranking mide variantes de catálogo disponibles, incluye stock cero y no representa ventas.
- No se añadió endpoint de borrado. Soft delete se comprobó en pruebas H2 transaccionales; el recorrido HTTP usó operaciones públicas existentes.
- La carga paginada usa varias sentencias bajo el aislamiento transaccional por defecto. No se promete una instantánea inmutable ante escrituras concurrentes.
- Swagger se verificó por sus recursos HTTP y el documento OpenAPI; no se hizo una inspección visual manual del navegador.
- Gradle emitió avisos de API obsoleta y de instrumentación de la JVM; no impidieron compilación ni pruebas.
