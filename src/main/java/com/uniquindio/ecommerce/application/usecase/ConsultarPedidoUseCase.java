package com.uniquindio.ecommerce.application.usecase;
import com.uniquindio.ecommerce.application.security.*;
import com.uniquindio.ecommerce.domain.entity.PedidoDeModa;
import com.uniquindio.ecommerce.domain.repository.PedidoDeModaRepository;
import com.uniquindio.ecommerce.domain.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class ConsultarPedidoUseCase {
    private final PedidoDeModaRepository pedidos;
    private final UsuarioActual actual;
    @Transactional(readOnly=true)
    public PedidoDeModa ejecutar(UUID id) {
        var pedido=pedidos.porId(id).orElseThrow(() -> new RecursoNoEncontradoException("El pedido no existe."));
        if(!actual.id().equals(pedido.getClienteId())) throw new AccesoDenegadoException();
        return pedido;
    }
}
