package view;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.scene.control.Label;

import java.util.Objects;

import static view.GameDialogs.*;

/**
 * Класс главного меню
 */
public class MainMenu extends Application {
    /**
     * Метод для запуска приложения
     * @param primaryStage первичная сцена
     */
    @Override
    public void start(Stage primaryStage) {
        // Контейнер
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root-pane");

        // Заголовок и описание
        Label titleLabel = new Label("MEMO BATTLE");
        titleLabel.getStyleClass().add("game-title");

        Label descLabel = new Label("Сразись в битве умов!\nНайди все пары быстрее соперника.");
        descLabel.getStyleClass().add("game-desc");
        descLabel.setTextAlignment(TextAlignment.CENTER);

        // Кнопки
        Button rulesButton = new Button("Правила");
        Button createGameButton = new Button("Создать игру");
        Button joinGameButton = new Button("Присоединиться к игре");

        // Инициализация действий кнопок
        rulesButton.setOnAction(_ -> showRules());
        createGameButton.setOnAction(_ -> showCreateGameDialog());
        joinGameButton.setOnAction(_ -> showConnectGameDialog());

        // Добавление элементов в контейнер
        root.getChildren().addAll(titleLabel, descLabel, rulesButton, createGameButton, joinGameButton);
        Scene scene = new Scene(root, 600, 500);

        // Загружаем стили
        try {
            String cssPath = Objects.requireNonNull(MainMenu.class.getResource("/style/main-styles.css")).toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.out.println("Ошибка загрузки стилей");
        }

        primaryStage.setTitle("Memo Battle");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Метод для запуска приложения
     * @param args аргументы (не используются)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
