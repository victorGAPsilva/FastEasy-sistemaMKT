package com.fasteasy.application.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspecto de logging para os serviços de aplicação.
 *
 * Registra a execução dos métodos e captura/loga erros de negócio,
 * centralizando o tratamento básico de erros sem poluir os serviços.
 */
@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Around("execution(* com.fasteasy.service..*(..))")
    public Object logarExecucao(ProceedingJoinPoint joinPoint) throws Throwable {
        String assinatura = joinPoint.getSignature().toShortString();
        long inicio = System.currentTimeMillis();
        try {
            Object resultado = joinPoint.proceed();
            long duracao = System.currentTimeMillis() - inicio;
            log.debug("{} executado com sucesso em {} ms", assinatura, duracao);
            return resultado;
        } catch (IllegalArgumentException e) {
            log.warn("Validação falhou em {}: {}", assinatura, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro ao executar {}: {}", assinatura, e.getMessage(), e);
            throw e;
        }
    }
}
