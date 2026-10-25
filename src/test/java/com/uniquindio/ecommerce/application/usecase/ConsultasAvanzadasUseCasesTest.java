package com.uniquindio.ecommerce.application.usecase;

import com.uniquindio.ecommerce.domain.entity.PrendaDeAutor;
import com.uniquindio.ecommerce.domain.query.*;
import com.uniquindio.ecommerce.domain.repository.ConsultaPrendasRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultasAvanzadasUseCasesTest {
    @Test
    void buscarDelegaCriteriosYConservaMetadatos() {
        // Arrange
        var repository = mock(ConsultaPrendasRepository.class);
        var consulta = new ConsultaPrendas("lino", UUID.randomUUID(), "COP", 2, 10);
        var esperada = new Pagina<PrendaDeAutor>(List.of(), 15, 2, 2, 10);
        when(repository.buscar(consulta)).thenReturn(esperada);
        // Act
        var resultado = new BuscarPrendasUseCase(repository).ejecutar(consulta);
        // Assert
        assertSame(esperada, resultado);
        verify(repository).buscar(consulta);
    }

    @Test
    void rankingDelegaSinInventarEstadisticas() {
        // Arrange
        var repository = mock(ConsultaPrendasRepository.class);
        var esperado = List.of(new VariedadCreador(UUID.randomUUID(), 7, 3));
        when(repository.rankingVariedad()).thenReturn(esperado);
        // Act
        var resultado = new ConsultarVariedadCreadoresUseCase(repository).ejecutar();
        // Assert
        assertSame(esperado, resultado);
        verify(repository).rankingVariedad();
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "0,101", "2147483647,2"})
    void criteriosRechazanPaginacionInvalidaSinSpring(int page, int size) {
        // Arrange: límites del contrato del dominio.
        // Act & Assert
        assertThrows(ConsultaInvalidaException.class, () -> new ConsultaPrendas(null, null, null, page, size));
    }
}
