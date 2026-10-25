package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.query.VariedadCreador;
import com.uniquindio.ecommerce.domain.repository.ConsultaPrendasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarVariedadCreadoresUseCase {
    private final ConsultaPrendasRepository repository;

    @Transactional(readOnly = true)
    public List<VariedadCreador> ejecutar() {
        return repository.rankingVariedad();
    }
}
