package com.uniquindio.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EcommerceApplicationTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	void contextLoads() {
		// Arrange: @SpringBootTest prepara e inyecta el contexto real de la aplicación.
		// Act
		boolean contextoActivo = context.isActive();
		// Assert
		assertTrue(contextoActivo, "El contexto de BOHEME debe estar activo tras arrancar.");
	}

}
