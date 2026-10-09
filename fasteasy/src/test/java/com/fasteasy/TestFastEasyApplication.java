package com.fasteasy;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração de bootstrap usada apenas pelos testes de persistência.
 *
 * Fica no pacote base {@code com.fasteasy} para que o {@code @DataJpaTest}
 * a encontre subindo a hierarquia de pacotes a partir dos testes.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.fasteasy.domain")
@EnableJpaRepositories(basePackages = "com.fasteasy.repository")
public class TestFastEasyApplication {
}
