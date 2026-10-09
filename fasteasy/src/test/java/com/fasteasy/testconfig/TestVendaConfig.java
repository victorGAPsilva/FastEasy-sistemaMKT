package com.fasteasy.testconfig;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração de contexto usada pelos testes de integração de vendas.
 *
 * <p>Restringe o escaneamento a serviços, repositórios e domínio, evitando
 * instanciar as telas JavaFX (que exigiriam o toolkit gráfico).</p>
 */
@SpringBootApplication(scanBasePackages = {"com.fasteasy.service", "com.fasteasy.application.logging"})
@EntityScan(basePackages = "com.fasteasy.domain")
@EnableJpaRepositories(basePackages = "com.fasteasy.repository")
public class TestVendaConfig {
}
