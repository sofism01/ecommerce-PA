package com.uniquindio.ecommerce.application.usecase;
import com.uniquindio.ecommerce.application.security.*;
import com.uniquindio.ecommerce.domain.repository.*;
import com.uniquindio.ecommerce.domain.valueobject.*;
import com.uniquindio.ecommerce.domain.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
/** La identidad de la cuenta coincide con creadorId/clienteId. Ningún UUID del cliente concede propiedad. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class AutorizarRecursosUseCase {
    private final UsuarioActual actual;
    private final PrendaDeAutorRepository prendas;
    private final BolsaDeEstiloRepository bolsas;
    public void identidad(UUID propietario) {
        if (!actual.id().equals(propietario)) throw new AccesoDenegadoException();
    }
    public void prenda(UUID id) {
        identidad(prendas.obtenerPrendaPorId(new PrendaDeAutorId(id))
                .orElseThrow(() -> new RecursoNoEncontradoException("La prenda no existe.")).getCreadorId());
    }
    public void bolsa(UUID id) {
        identidad(bolsas.obtenerBolsaPorId(new BolsaDeEstiloId(id))
                .orElseThrow(() -> new RecursoNoEncontradoException("La bolsa no existe.")).getClienteId());
    }
}
