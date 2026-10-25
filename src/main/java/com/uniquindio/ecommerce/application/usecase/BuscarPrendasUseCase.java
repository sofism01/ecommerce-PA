package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.query.*;
import com.uniquindio.ecommerce.domain.repository.ConsultaPrendasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuscarPrendasUseCase {
    private final ConsultaPrendasRepository repository;

    @Transactional(readOnly = true)
    public Pagina<PrendaDeAutor> ejecutar(ConsultaPrendas consulta) {
        return repository.buscar(consulta);
    }
}
