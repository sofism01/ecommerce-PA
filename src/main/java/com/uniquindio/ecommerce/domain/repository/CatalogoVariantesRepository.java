package com.uniquindio.ecommerce.domain.repository;
import com.uniquindio.ecommerce.domain.valueobject.PrendaDeAutorId;
import java.util.Optional;
import java.util.UUID;
public interface CatalogoVariantesRepository {
    Optional<PrendaDeAutorId> prendaDeVariante(UUID varianteId);
}
