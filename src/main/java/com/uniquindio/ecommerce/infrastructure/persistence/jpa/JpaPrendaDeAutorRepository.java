package com.uniquindio.ecommerce.infrastructure.persistence.jpa;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.exception.RecursoNoEncontradoException;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.repository.ConsultaPrendasRepository;
import com.uniquindio.ecommerce.domain.query.*;
import org.springframework.data.domain.PageRequest;
import java.util.Locale;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.mapper.PrendaJpaMapper;
import com.uniquindio.ecommerce.infrastructure.persistence.jpa.repository.PrendaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaPrendaDeAutorRepository implements PrendaDeAutorRepository, ConsultaPrendasRepository,
        com.uniquindio.ecommerce.domain.repository.CatalogoVariantesRepository {
    private final PrendaJpaRepository repository;
    private final PrendaJpaMapper mapper;

    @Override
    public Optional<PrendaDeAutorId> prendaDeVariante(UUID varianteId) {
        return repository.prendaDeVariante(varianteId).map(PrendaDeAutorId::new);
    }

    @Override
    @Transactional
    public Optional<PrendaDeAutor> obtenerPrendaParaActualizar(PrendaDeAutorId id) {
        return repository.bloquear(id.value()).map(mapper::toDomain);
    }

    @Override
    public Pagina<PrendaDeAutor> buscar(ConsultaPrendas consulta) {
        String texto = consulta.q() == null ? null : "%" + consulta.q().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String moneda = consulta.moneda() == null ? null : consulta.moneda().toUpperCase(Locale.ROOT);
        var ids = repository.buscarIds(texto, consulta.creadorId(), moneda,
                PageRequest.of(consulta.page(), consulta.size()));
        var content = ids.isEmpty() ? List.<PrendaDeAutor>of()
                : repository.cargarPaginaConVariantes(ids.getContent()).stream().map(mapper::toDomain).toList();
        return new Pagina<>(content, ids.getTotalElements(), ids.getTotalPages(), consulta.page(), consulta.size());
    }

    @Override
    public List<VariedadCreador> rankingVariedad() {
        return repository.rankingVariedad();
    }

    @Override
    public Optional<PrendaDeAutor> obtenerPrendaPorId(PrendaDeAutorId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<PrendaDeAutor> obtenerPrendasPorCreador(UUID creadorId) {
        return repository.findByCreadorIdOrderByIdAsc(creadorId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public PrendaDeAutor guardarPrenda(PrendaDeAutor dominio) {
        var existente = repository.findById(dominio.getId().value());
        if (existente.isPresent()) {
            mapper.actualizar(dominio, existente.get());
            return mapper.toDomain(existente.get());
        }
        if (repository.contarIncluyendoEliminados(dominio.getId().value()) != 0) {
            throw new RecursoNoEncontradoException("La prenda está eliminada y no puede sobrescribirse.");
        }
        return mapper.toDomain(repository.save(mapper.toJpa(dominio)));
    }

    /** Capacidad de infraestructura para 2.5; no añade endpoints ni casos de uso. */
    @Transactional
    public void eliminarLogicamente(PrendaDeAutorId id) {
        repository.findById(id.value()).ifPresent(repository::delete);
    }
}
