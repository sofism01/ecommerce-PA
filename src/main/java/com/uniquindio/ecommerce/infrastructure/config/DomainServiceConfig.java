package com.uniquindio.ecommerce.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.uniquindio.ecommerce.domain.repository.PrendaDeAutorRepository;
import com.uniquindio.ecommerce.domain.repository.BolsaDeEstiloRepository;
import com.uniquindio.ecommerce.domain.service.ActualizadorDeStock;
import com.uniquindio.ecommerce.domain.service.GestorDeBolsaDeEstilo;

@Configuration
public class DomainServiceConfig {

    @Bean
    public ActualizadorDeStock actualizadorDeStock(PrendaDeAutorRepository prendaDeAutorRepository) {
        return new ActualizadorDeStock(prendaDeAutorRepository);
    }

    @Bean
    public GestorDeBolsaDeEstilo gestorDeBolsaDeEstilo(BolsaDeEstiloRepository bolsaDeEstiloRepository) {
        return new GestorDeBolsaDeEstilo(bolsaDeEstiloRepository);
    }
}