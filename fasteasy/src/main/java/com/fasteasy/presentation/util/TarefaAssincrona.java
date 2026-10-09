package com.fasteasy.presentation.util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Executor de tarefas de banco de dados fora da thread da interface JavaFX.
 *
 * <p>Garante que nenhuma consulta a banco seja executada na thread do
 * JavaFX Application Thread: o trabalho roda em um pool de threads daemon e
 * os callbacks de sucesso/erro são devolvidos à thread da UI via
 * {@link Platform#runLater(Runnable)}.</p>
 */
public final class TarefaAssincrona {

    private static final Logger log = LoggerFactory.getLogger(TarefaAssincrona.class);

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "fasteasy-db");
        t.setDaemon(true);
        return t;
    });

    private TarefaAssincrona() {
    }

    /**
     * Executa um trabalho em background e entrega o resultado à UI.
     *
     * @param trabalho  operação de banco/serviço executada fora da UI
     * @param onSucesso callback na thread da UI com o resultado
     * @param onErro    callback na thread da UI em caso de falha
     */
    public static <T> void executar(Supplier<T> trabalho,
                                    Consumer<T> onSucesso,
                                    Consumer<Throwable> onErro) {
        POOL.submit(() -> {
            try {
                T resultado = trabalho.get();
                Platform.runLater(() -> {
                    try {
                        onSucesso.accept(resultado);
                    } catch (Exception e) {
                        log.error("Falha ao atualizar a interface após operação assíncrona", e);
                    }
                });
            } catch (Throwable e) {
                log.warn("Operação assíncrona falhou: {}", e.getMessage());
                Platform.runLater(() -> onErro.accept(e));
            }
        });
    }
}
