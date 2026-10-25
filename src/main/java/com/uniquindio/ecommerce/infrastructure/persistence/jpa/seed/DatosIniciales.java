package com.uniquindio.ecommerce.infrastructure.persistence.jpa.seed;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.entity.VariantePrenda;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository.BolsaJpaRepository;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository.PrendaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "boheme.datos-iniciales.enabled", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {
    public static final UUID CREADOR_UNO = UUID.fromString("81000000-0000-4000-8000-000000000001");
    public static final UUID CREADOR_DOS = UUID.fromString("81000000-0000-4000-8000-000000000002");
    public static final UUID CLIENTE_UNO = UUID.fromString("c1000000-0000-4000-8000-000000000001");
    public static final UUID CLIENTE_DOS = UUID.fromString("c1000000-0000-4000-8000-000000000002");
    private final PrendaDeAutorRepository prendas;
    private final BolsaDeEstiloRepository bolsas;
    private final PrendaJpaRepository filasPrendas;
    private final BolsaJpaRepository filasBolsas;

    @Override
    @Transactional
    public void run(String... args) {
        guardarSiFalta(PrendaDeAutor.crear(prendaId(1), CREADOR_UNO, "Camisa de lino artesanal",
                "Camisa de autor confeccionada en lino", dinero("150000.00", "COP"), List.of(
                        variante(1, "BOH-LINO-M", TipoTalla.M, 5),
                        variante(2, "BOH-LINO-L", TipoTalla.L, 3)), 8));
        guardarSiFalta(PrendaDeAutor.crear(prendaId(2), CREADOR_UNO, "Vestido de algodón",
                "Vestido de colección independiente", dinero("220000.00", "COP"), List.of(
                        variante(3, "BOH-VEST-S", TipoTalla.S, 4),
                        variante(4, "BOH-VEST-M", TipoTalla.M, 0)), 4));
        guardarSiFalta(PrendaDeAutor.crear(prendaId(3), CREADOR_DOS, "Chaqueta de autor",
                "Chaqueta de edición limitada", dinero("95.90", "USD"), List.of(
                        variante(5, "BOH-CHAQ-L", TipoTalla.L, 2)), 2));

        if (filasBolsas.contarIncluyendoEliminados(bolsaId(1).valor()) == 0) {
            var bolsa = BolsaDeEstilo.crear(bolsaId(1), CLIENTE_UNO);
            agregarLinea(bolsa, prendaId(1), 0, 2);
            agregarLinea(bolsa, prendaId(2), 0, 1);
            bolsas.guardarBolsa(bolsa);
        }
        if (filasBolsas.contarIncluyendoEliminados(bolsaId(2).valor()) == 0) {
            var bolsa = BolsaDeEstilo.crear(bolsaId(2), CLIENTE_DOS);
            agregarLinea(bolsa, prendaId(1), 1, 1);
            bolsa.cerrarBolsa();
            bolsas.guardarBolsa(bolsa);
        }
    }

    private void guardarSiFalta(PrendaDeAutor prenda) {
        // Incluye tombstones: ejecutar otra vez no duplica ni resucita datos eliminados.
        if (filasPrendas.contarIncluyendoEliminados(prenda.getId().value()) == 0) {
            prendas.guardarPrenda(prenda);
        }
    }

    private void agregarLinea(BolsaDeEstilo bolsa, PrendaDeAutorId prendaId, int indice, int cantidad) {
        var prenda = prendas.obtenerPrendaPorId(prendaId).orElseThrow();
        var variante = prenda.getVariantes().get(indice);
        bolsa.agregarLinea(LineaDeBolsa.crear(variante.id(), cantidad, prenda.getPrecioBase()), variante.stockDisponible());
    }

    private static PrendaDeAutorId prendaId(int numero) {
        return new PrendaDeAutorId(UUID.fromString("91000000-0000-4000-8000-%012d".formatted(numero)));
    }

    private static BolsaDeEstiloId bolsaId(int numero) {
        return new BolsaDeEstiloId(UUID.fromString("b1000000-0000-4000-8000-%012d".formatted(numero)));
    }

    private static VariantePrenda variante(int numero, String sku, TipoTalla talla, int stock) {
        return VariantePrenda.crear(UUID.fromString("a1000000-0000-4000-8000-%012d".formatted(numero)), new SKU(sku), talla, stock);
    }

    private static Dinero dinero(String monto, String moneda) {
        return new Dinero(new BigDecimal(monto), moneda);
    }
}
