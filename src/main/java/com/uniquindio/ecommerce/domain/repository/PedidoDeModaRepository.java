package com.uniquindio.ecommerce.domain.repository;
import com.uniquindio.ecommerce.domain.entity.PedidoDeModa;
import java.util.Optional;
import java.util.UUID;
public interface PedidoDeModaRepository {
    PedidoDeModa registrar(PedidoDeModa pedido);
    Optional<PedidoDeModa> porId(UUID id);
}
