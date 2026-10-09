package com.fasteasy.application;

import com.fasteasy.config.FastEasyConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Objects;

/**
 * Ponto de entrada do FastEasy.
 *
 * Integra o ciclo de vida do JavaFX com o do Spring Boot:
 * <ul>
 *     <li>O contexto Spring é criado em {@link #init()} (antes da UI).</li>
 *     <li>A janela principal é montada em {@link #start(Stage)}.</li>
 *     <li>O contexto Spring é encerrado em {@link #stop()}.</li>
 * </ul>
 */
public class FastEasyApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        // Cria o contexto Spring antes de qualquer componente JavaFX.
        this.applicationContext = new SpringApplicationBuilder(FastEasyConfig.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(applicationContext::getBean);
        loader.setLocation(Objects.requireNonNull(
                getClass().getResource("/view/main-view.fxml")));

        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/css/fasteasy.css")).toExternalForm());

        primaryStage.setTitle("FastEasy - Gestão de Supermercado");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
