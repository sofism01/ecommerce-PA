package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.domain.entity.BolsaDeEstilo;
import com.uniquindio.ecommerce.domain.exception.BolsaDeEstiloNoEncontradaException;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.valueobject.BolsaDeEstiloId;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper.BolsaJpaMapper;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository.BolsaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaBolsaDeEstiloRepository implements BolsaDeEstiloRepository {
    private final BolsaJpaRepository repository;
    private final BolsaJpaMapper mapper;

    @Override
    @Transactional
    public Optional<BolsaDeEstilo> obtenerBolsaParaActualizar(BolsaDeEstiloId id) {
        return repository.bloquear(id.valor()).map(mapper::toDomain);
    }

    @Override
    public Optional<BolsaDeEstilo> obtenerBolsaPorId(BolsaDeEstiloId id) {
        return repository.findById(id.valor()).map(mapper::toDomain);
    }

    @Override
    public Optional<BolsaDeEstilo> obtenerBolsaActivaPorCliente(UUID clienteId) {
        return repository.findByClienteIdAndCerradaFalseOrderByIdAsc(clienteId).stream().findFirst().map(mapper::toDomain);
    }

    @Override
    @Transactional
    public BolsaDeEstilo guardarBolsa(BolsaDeEstilo dominio) {
        var existente = repository.findById(dominio.getId().valor());
        if (existente.isPresent()) {
            mapper.actualizar(dominio, existente.get());
            return mapper.toDomain(existente.get());
        }
        if (repository.contarIncluyendoEliminados(dominio.getId().valor()) != 0) {
            throw new BolsaDeEstiloNoEncontradaException("La bolsa está eliminada y no puede sobrescribirse.");
        }
        return mapper.toDomain(repository.save(mapper.toJpa(dominio)));
    }

    @Transactional
    public void eliminarLogicamente(BolsaDeEstiloId id) {
        repository.findById(id.valor()).ifPresent(repository::delete);
    }
}
