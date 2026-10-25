package com.uniquindio.ecommerce.infrastructure.persistence.jpa;
import com.uniquindio.ecommerce.domain.entity.PedidoDeModa;
import com.uniquindio.ecommerce.domain.repository.PedidoDeModaRepository;
import com.uniquindio.ecommerce.domain.exception.ReglaDominioException;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository.PedidoJpaRepository;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper.PedidoJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
@Repository @RequiredArgsConstructor @Transactional(readOnly=true)
public class JpaPedidoDeModaRepository implements PedidoDeModaRepository {
    private final PedidoJpaRepository repository;
    private final PedidoJpaMapper mapper;
    @Transactional
    public PedidoDeModa registrar(PedidoDeModa p) {
        try {
            var e=repository.save(mapper.toJpa(p)); repository.flush(); return mapper.toDomain(e);
        } catch(DataIntegrityViolationException ex) {
            if(ex.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT).contains("uk_pedido_bolsa"))
                throw new ReglaDominioException("La bolsa ya tiene un pedido confirmado.");
            throw ex;
        }
    }
    public Optional<PedidoDeModa> porId(UUID id) { return repository.findById(id).map(mapper::toDomain); }
}
