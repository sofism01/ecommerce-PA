package com.uniquindio.ecommerce.application.usecase;
import com.uniquindio.ecommerce.application.security.UsuarioActual;
import com.uniquindio.ecommerce.application.security.AccesoDenegadoException;
import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.repository.*;
import com.uniquindio.ecommerce.domain.exception.*;
import com.uniquindio.ecommerce.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class AgregarVarianteABolsaUseCase {
    private final BolsaDeEstiloRepository bolsas;
    private final PrendaDeAutorRepository prendas;
    private final CatalogoVariantesRepository catalogo;
    private final UsuarioActual actual;
    @Transactional
    public BolsaDeEstilo ejecutar(UUID bolsaId,UUID varianteId,int cantidad) {
        var bolsa=bolsas.obtenerBolsaParaActualizar(new BolsaDeEstiloId(bolsaId))
                .orElseThrow(() -> new RecursoNoEncontradoException("La bolsa no existe."));
        if(!actual.id().equals(bolsa.getClienteId())) throw new AccesoDenegadoException();
        if(!bolsa.estaActiva()) throw new ModificacionBolsaNoPermitidaException("La bolsa está cerrada.");
        if(cantidad<=0) throw new ReglaDominioException("La cantidad debe ser positiva.");
        var prendaId=catalogo.prendaDeVariante(varianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La variante no existe."));
        var prenda=prendas.obtenerPrendaParaActualizar(prendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La prenda no existe."));
        if(!prenda.isDisponibleParaLaVenta()) throw new ReglaDominioException("La prenda está suspendida para la venta.");
        var variante=prenda.getVariantes().stream().filter(v -> v.id().equals(varianteId)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La variante no existe."));
        bolsa.agregarLinea(LineaDeBolsa.crear(varianteId,cantidad,prenda.getPrecioBase()),variante.stockDisponible());
        return bolsas.guardarBolsa(bolsa);
    }
}
