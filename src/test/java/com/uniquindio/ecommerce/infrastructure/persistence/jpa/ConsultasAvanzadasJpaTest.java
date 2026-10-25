package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.domain.entity.*;
import com.uniquindio.ecommerce.domain.query.*;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true"})
@Import({JpaPrendaDeAutorRepository.class, PrendaJpaMapper.class, DineroJpaMapper.class,
        ConsultasAvanzadasJpaTest.SqlConfig.class})
class ConsultasAvanzadasJpaTest {
    static final UUID A = new UUID(0, 1);
    static final UUID B = new UUID(0, 2);
    static final UUID C = new UUID(0, 3);
    static final List<String> SQL = new CopyOnWriteArrayList<>();
    @Autowired JpaPrendaDeAutorRepository prendas;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired JdbcTemplate jdbc;

    @TestConfiguration(proxyBeanMethods = false)
    static class SqlConfig {
        @Bean
        HibernatePropertiesCustomizer inspector() {
            return properties -> properties.put("hibernate.session_factory.statement_inspector",
                    (StatementInspector) sql -> { SQL.add(sql); return sql; });
        }
    }

    private PrendaDeAutor guardar(int id, UUID creador, String nombre, String descripcion, String moneda, int variantes) {
        var tallas = IntStream.range(0, variantes).mapToObj(i -> VariantePrenda.crear(
                UUID.randomUUID(), new SKU("SKU-" + id + "-" + i), TipoTalla.M, i)).toList();
        return prendas.guardarPrenda(PrendaDeAutor.crear(new PrendaDeAutorId(new UUID(0, id)),
                creador, nombre, descripcion, new Dinero(new BigDecimal("20.00"), moneda), tallas, 5));
    }

    private void recargar() { em.flush(); em.clear(); SQL.clear(); }
    private Pagina<PrendaDeAutor> buscar(String texto, UUID creador, String moneda, int page, int size) {
        return prendas.buscar(new ConsultaPrendas(texto, creador, moneda, page, size));
    }

    @BeforeEach
    void preparar() {
        guardar(11, A, "Camisa", "Lino artesanal", "COP", 2);
        guardar(12, A, "Vestido", "Algodón", "COP", 2);
        guardar(13, B, "Chaqueta", "Edición limitada", "USD", 1);
        recargar();
    }

    @ParameterizedTest
    @CsvSource({"Camisa,Camisa", "artesanal,Camisa", "cAmIsA,Camisa", "LINO,Camisa", "edición,Chaqueta"})
    void buscaPorNombreODescripcionSinDistinguirMayusculas(String q, String nombre) {
        // Arrange: datos persistidos y contexto vacío en preparar().
        // Act
        var pagina = buscar(q, null, null, 0, 10);
        // Assert
        assertEquals(List.of(nombre), pagina.content().stream().map(PrendaDeAutor::getNombre).toList());
        assertEquals(1, pagina.totalElements());
        assertEquals(1, pagina.totalPages());
    }

    @Test
    void filtraPorCreadorEnH2() {
        // Arrange
        var consulta = new ConsultaPrendas(null, A, null, 0, 10);
        // Act
        var pagina = prendas.buscar(consulta);
        // Assert
        assertEquals(2, pagina.totalElements());
        assertTrue(pagina.content().stream().allMatch(p -> p.getCreadorId().equals(A)));
    }

    @Test
    void filtraPorMonedaYNormalizaEntrada() {
        // Arrange
        var consulta = new ConsultaPrendas(null, null, " usd ", 0, 10);
        // Act
        var pagina = prendas.buscar(consulta);
        // Assert
        assertEquals(1, pagina.totalElements());
        assertEquals("Chaqueta", pagina.content().getFirst().getNombre());
    }

    @Test
    void combinaBusquedaCreadorYMonedaConAnd() {
        // Arrange
        var consulta = new ConsultaPrendas(" artesanal ", A, "cop", 0, 1);
        // Act
        var pagina = prendas.buscar(consulta);
        // Assert
        assertEquals(1, pagina.totalElements());
        assertEquals("Camisa", pagina.content().getFirst().getNombre());
        assertTrue(buscar("artesanal", B, "COP", 0, 1).content().isEmpty());
    }

    @Test
    void paginaConConteoTotalSinDuplicadosYConTodasLasVariantes() {
        // Arrange
        var esperados = List.of("Camisa", "Chaqueta", "Vestido");
        // Act & Assert
        for (int page = 0; page < 3; page++) {
            var pagina = buscar(null, null, null, page, 1);
            assertEquals(3, pagina.totalElements());
            assertEquals(3, pagina.totalPages());
            assertEquals(page, pagina.page());
            assertEquals(1, pagina.size());
            assertEquals(esperados.get(page), pagina.content().getFirst().getNombre());
            assertEquals(page == 1 ? 1 : 2, pagina.content().getFirst().getVariantes().size());
        }
        var ultima = buscar(null, null, null, 1, 2);
        assertEquals(3, ultima.totalElements());
        assertEquals(2, ultima.totalPages());
        assertEquals(1, ultima.content().size());
    }

    @Test
    void desempataNombresPorIdEnBaseDeDatos() {
        // Arrange: inserción en orden inverso.
        guardar(22, A, "Igual", "Dos", "COP", 1);
        guardar(21, A, "Igual", "Uno", "COP", 1);
        recargar();
        // Act
        var primera = buscar("Igual", null, null, 0, 1);
        var segunda = buscar("Igual", null, null, 1, 1);
        // Assert
        assertEquals(new UUID(0, 21), primera.content().getFirst().getId().value());
        assertEquals(new UUID(0, 22), segunda.content().getFirst().getId().value());
    }

    @Test
    void sinCoincidenciasYFueraDeRangoConservaConteos() {
        // Arrange & Act
        var vacia = buscar("inexistente", null, null, 0, 2);
        var fuera = buscar(null, null, null, 8, 2);
        // Assert
        assertTrue(vacia.content().isEmpty());
        assertEquals(0, vacia.totalElements());
        assertEquals(0, vacia.totalPages());
        assertTrue(fuera.content().isEmpty());
        assertEquals(3, fuera.totalElements());
        assertEquals(2, fuera.totalPages());
        assertEquals(8, fuera.page());
    }

    @Test
    void omiteTextoYMonedaBlancosYEscapaComodines() {
        // Arrange
        guardar(20, A, "100%_! autor", "Literal", "COP", 1);
        recargar();
        // Act
        var literal = buscar("%_!", null, null, 0, 10);
        var todas = buscar("  ", null, " ", 0, 10);
        // Assert
        assertEquals(1, literal.totalElements());
        assertEquals(4, todas.totalElements());
    }

    @Test
    void excluyePrendasEliminadasDelContenidoConteoYRanking() {
        // Arrange
        var id = new PrendaDeAutorId(new UUID(0, 11));
        prendas.eliminarLogicamente(id);
        recargar();
        // Act
        var pagina = buscar(null, null, null, 0, 1);
        var ranking = prendas.rankingVariedad();
        // Assert
        assertEquals(2, pagina.totalElements());
        assertEquals("Chaqueta", pagina.content().getFirst().getNombre());
        assertEquals(new VariedadCreador(A, 2, 1), ranking.getFirst());
        assertEquals(1, jdbc.queryForObject("select count(*) from prendas_autor where id = ? and eliminado = true", Integer.class, id.value()));
    }

    @Test
    void rankingCuentaVariantesYPrendasDistintasCoincideConSqlReal() {
        // Arrange: A tiene dos prendas y cuatro variantes, B una y una.
        // Act
        var ranking = prendas.rankingVariedad();
        var sql = jdbc.query("""
                select p.creador_id, count(v.id), count(distinct p.id)
                from prendas_autor p join variantes_prenda v on v.prenda_id=p.id
                where p.eliminado=false and v.eliminado=false and p.disponible_para_venta=true
                group by p.creador_id order by count(v.id) desc, p.creador_id asc
                """, (rs, row) -> new VariedadCreador(rs.getObject(1, UUID.class), rs.getLong(2), rs.getLong(3)));
        // Assert
        assertEquals(List.of(new VariedadCreador(A, 4, 2), new VariedadCreador(B, 1, 1)), ranking);
        assertEquals(sql, ranking);
    }

    @Test
    void excluyeVarianteHuerfanaEliminadaSinPerderPrenda() {
        // Arrange
        var original = prendas.obtenerPrendaPorId(new PrendaDeAutorId(new UUID(0, 11))).orElseThrow();
        prendas.guardarPrenda(PrendaDeAutor.reconstruir(original.getId(), A, original.getNombre(),
                original.getDescripcion(), original.getPrecioBase(), List.of(original.getVariantes().getFirst()), true, 5));
        recargar();
        // Act
        var ranking = prendas.rankingVariedad();
        var pagina = buscar("Camisa", null, null, 0, 10);
        // Assert
        assertEquals(new VariedadCreador(A, 3, 2), ranking.getFirst());
        assertEquals(1, pagina.content().getFirst().getVariantes().size());
        assertEquals(1, jdbc.queryForObject("select count(*) from variantes_prenda where eliminado=true", Integer.class));
    }

    @Test
    void desempataRankingPorCreadorAscendente() {
        // Arrange
        guardar(20, C, "Otra", "Tercero", "USD", 1);
        recargar();
        // Act
        var ranking = prendas.rankingVariedad();
        // Assert
        assertEquals(List.of(A, B, C), ranking.stream().map(VariedadCreador::creadorId).toList());
    }

    @Test
    void suspensionExcluyeOfertaDelRankingPeroNoDelListado() {
        // Arrange
        var id = new PrendaDeAutorId(new UUID(0, 13));
        prendas.guardarPrenda(prendas.obtenerPrendaPorId(id).orElseThrow().suspenderVenta());
        recargar();
        // Act
        var ranking = prendas.rankingVariedad();
        var pagina = buscar(null, null, null, 0, 10);
        // Assert
        assertEquals(List.of(new VariedadCreador(A, 4, 2)), ranking);
        assertEquals(3, pagina.totalElements());
    }

    @Test
    void rankingVacioSiNoHayOferta() {
        // Arrange
        for (int id : List.of(11, 12, 13)) prendas.eliminarLogicamente(new PrendaDeAutorId(new UUID(0, id)));
        recargar();
        // Act
        var ranking = prendas.rankingVariedad();
        // Assert
        assertTrue(ranking.isEmpty());
    }

    @Test
    void ejecutaLimiteEnSqlYNumeroConstanteDeConsultasSinNMasUno() {
        // Arrange: diez prendas con múltiples variantes, fuera del contexto de primer nivel.
        for (int id = 30; id < 40; id++) guardar(id, A, "Prenda " + id, "Lino", "COP", 3);
        recargar();
        var stats = emf.unwrap(SessionFactoryImplementor.class).getStatistics();
        stats.clear();
        // Act
        var pagina = buscar(null, null, null, 1, 5);
        // Assert: IDs limitados, COUNT, y un solo fetch de las variantes de esa página.
        assertEquals(13, pagina.totalElements());
        assertEquals(5, pagina.content().size());
        assertEquals(3, stats.getPrepareStatementCount());
        assertTrue(SQL.stream().anyMatch(sql -> sql.contains("offset") && sql.contains("fetch first")), SQL.toString());
        assertEquals(1, SQL.stream().filter(sql -> sql.contains("left join variantes_prenda")).count());
        assertTrue(SQL.stream().anyMatch(sql -> sql.contains("lower(") && sql.contains("creador_id")));
    }
}
