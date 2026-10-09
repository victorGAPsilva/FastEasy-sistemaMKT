package com.fasteasy.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração central do FastEasy.
 * Habilita o Spring Boot, escaneamento de componentes, entidades JPA
 * e repositórios da aplicação.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.fasteasy")
@EntityScan(basePackages = "com.fasteasy.domain")
@EnableJpaRepositories(basePackages = "com.fasteasy.repository")
public class FastEasyConfig {
}
