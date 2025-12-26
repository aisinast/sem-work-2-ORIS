package view;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.ConnectionInfo;
import util.ConnectionHelper;

import java.util.Optional;

/**
 * Класс для отображения диалоговых окон
 */
public class GameDialogs {
    /**
     * Метод для отображения диалогового окна создания игры
     */
    public static void showCreateGameDialog() {
        // Создает диалоговое окно, типизированное ConnectionInfo
        Dialog<ConnectionInfo> dialog = new Dialog<>();
        dialog.setTitle("Вход в игру");
        dialog.setHeaderText("Подключение к серверу");

        // Настраиваем кнопки
        ButtonType createButtonType = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Создаем поля и метки
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField portField = new TextField();
        portField.setPromptText("8080");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Player1");

        gridPane.add(new Label("Порт:"), 0, 1);
        gridPane.add(portField, 1, 1);
        gridPane.add(new Label("Имя:"), 0, 2);
        gridPane.add(usernameField, 1, 2);

        dialog.getDialogPane().setContent(gridPane);

        javafx.application.Platform.runLater(portField::requestFocus); // Устанавливаем фокус на поле домена

        // Конвертируем результат диалогового окна в ConnectionInfo
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return new ConnectionInfo(
                        "localhost",
                        Integer.parseInt(portField.getText()),
                        usernameField.getText());
            }

            return null;
        });

        Optional<ConnectionInfo> result = dialog.showAndWait();

        result.ifPresent(ConnectionHelper::startServer); // Подключаемся к серверу
    }

    /**
     * Метод для отображения диалогового окна подключения к игре
     */
    public static void showConnectGameDialog() {
        // Создает диалоговое окно, типизированное ConnectionInfo
        Dialog<ConnectionInfo> dialog = new Dialog<>();
        dialog.setTitle("Подключение к игре");
        dialog.setHeaderText("Введите данные для подключения");

        // Настраиваем кнопки
        ButtonType connectButtonType = new ButtonType("Подключиться", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Создаем поля и метки
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField domainField = new TextField();
        domainField.setPromptText("127.0.0.1");
        TextField portField = new TextField();
        portField.setPromptText("8080");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Player1");

        gridPane.add(new Label("Домен:"), 0, 0);
        gridPane.add(domainField, 1, 0);
        gridPane.add(new Label("Порт:"), 0, 1);
        gridPane.add(portField, 1, 1);
        gridPane.add(new Label("Имя:"), 0, 2);
        gridPane.add(usernameField, 1, 2);

        dialog.getDialogPane().setContent(gridPane);

        javafx.application.Platform.runLater(domainField::requestFocus); // Устанавливаем фокус на поле домена

        // Конвертируем результат диалогового окна в ConnectionInfo
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new ConnectionInfo(
                        domainField.getText(),
                        Integer.parseInt(portField.getText()),
                        usernameField.getText());
            }

            return null;
        });

        Optional<ConnectionInfo> result = dialog.showAndWait();

        result.ifPresent(ConnectionHelper::connectClient); // Подключаемся к серверу
    }

    /**
     * Метод для отображения диалогового окна с правилами игры
     */
    public static void showRules() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Правила игры");
        alert.setHeaderText("Как играть в Memo Battle");
        alert.setContentText(
                """
                        1. Игроки ходят по очереди.
                        2. В свой ход можно открыть 2 карточки.
                        3. Если картинки совпали — получаешь очко и ходишь снова.
                        4. Если нет — ход переходит сопернику.
                        5. Побеждает тот, кто наберет больше очков."""
        );
        alert.showAndWait();
    }

    /**
     * Метод для отображения диалогового окна с ошибкой
     * @param message сообщение об ошибке
     */
    public static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Метод для отображения диалогового окна с уведомлением
     * @param message сообщение о событии
     */
    public static void showInfoDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Уведомление");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Метод для отображения диалогового окна с результатами игры
     * @param winners строка с именами победителей
     * @param score набранные очки
     */
    public static void showGameOverDialog(String winners, int score) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Игра окончена!");

        String resultString = winners.replaceAll(";", ", ");
        alert.setHeaderText("Победитель(-и): " + resultString.substring(0, resultString.length() - 2));
        alert.setContentText("Набрано очков: " + score + "\n\nПоздравляем!");

        // Ждем, пока пользователь закроет диалоговое окно
        alert.showAndWait();
    }
}
