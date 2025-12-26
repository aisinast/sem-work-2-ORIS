package view;

import client.GameClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для отображения игрового поля и интерфейса
 */
public class GameView extends Application {

    private final GameClient gameClient;
    private GameBoard gameBoard;

    // UI элементы
    private ListView<String> chatListView;
    private TextField chatInputField;
    private Button startButton;
    private VBox playerListContainer;

    private boolean isGameStarted = false;
    private String currentPlayerName = "";

    public GameView(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    /**
     * Метод для запуска JavaFX приложения
     * @param stage основной контейнер приложения
     */
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // Центр - игровое поле
        gameBoard = new GameBoard(gameClient);
        VBox boardContainer = new VBox(gameBoard);
        boardContainer.setAlignment(Pos.CENTER);
        // Делаем контейнер прозрачным, чтобы видеть фон root
        boardContainer.setStyle("-fx-background-color: transparent;");
        root.setCenter(boardContainer);

        // Правая панель со статистикой и чатом
        VBox rightPanel = new VBox(20);
        rightPanel.setPadding(new Insets(20));
        rightPanel.setPrefWidth(320);
        rightPanel.setStyle("-fx-background-color: transparent;");

        // Блок статистики (игроки)
        Label statsTitle = new Label("ИГРОКИ");
        statsTitle.getStyleClass().add("game-title");

        playerListContainer = new VBox(8);
        playerListContainer.setPadding(new Insets(5));

        VBox statsBox = new VBox(10, statsTitle, new Separator(), playerListContainer);
        statsBox.getStyleClass().add("game-panel");

        // Блок чата
        Label chatTitle = new Label("ЧАТ");
        chatTitle.getStyleClass().add("game-title");

        chatListView = new ListView<>();
        chatListView.setPrefHeight(300);
        chatListView.getStyleClass().add("chat-list-view");

        chatInputField = new TextField();
        chatInputField.setPromptText("Напишите сообщение...");
        chatInputField.getStyleClass().add("chat-input");
        chatInputField.setOnAction(_ -> sendChatMessage());

        Button sendButton = new Button("➤");
        sendButton.getStyleClass().add("send-button");
        sendButton.setOnAction(_ -> sendChatMessage());

        HBox chatInputBox = new HBox(5, chatInputField, sendButton);
        chatInputBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(chatInputField, Priority.ALWAYS);

        // Кнопки управления игрой (начать игру, покинуть игру)
        startButton = new Button("НАЧАТЬ ИГРУ");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.getStyleClass().addAll("game-button", "btn-start");
        startButton.setOnAction(_ -> {
            if (gameClient != null) gameClient.sendStartGameMessage();
        });

        Button leaveButton = new Button("ПОКИНУТЬ ИГРУ");
        leaveButton.setMaxWidth(Double.MAX_VALUE);
        leaveButton.getStyleClass().addAll("game-button", "btn-leave");
        leaveButton.setOnAction(_ -> {
            if (gameClient != null) gameClient.disconnect();
            Platform.exit();
            System.exit(0);
        });

        VBox chatBox = new VBox(10, chatTitle, chatListView, chatInputBox, new Separator(), startButton, leaveButton);
        chatBox.getStyleClass().add("game-panel");

        rightPanel.getChildren().addAll(statsBox, chatBox);
        VBox.setVgrow(statsBox, Priority.ALWAYS);

        root.setRight(rightPanel);

        Scene scene = new Scene(root, 1100, 800);

        // Подключение css
        try {
            String css = Objects.requireNonNull(getClass().getResource("/style/game-styles.css")).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Не удалось загрузить CSS! Проверьте путь к файлу.");
        }

        // Настройка окна
        stage.setTitle("Memo Battle");
        stage.setScene(scene);
        stage.show();

        // Обработка закрытия окна
        stage.setOnCloseRequest(_ -> {
            if (gameClient != null) gameClient.disconnect();
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Метод для отправки сообщения в чат
     */
    private void sendChatMessage() {
        String text = chatInputField.getText().trim();
        if (!text.isEmpty() && gameClient != null) {
            gameClient.sendChatMessage(text); // Отправляем сообщение на сервер через клиента
            chatInputField.clear(); // Очищаем поле ввода
        }
    }

    /**
     * Метод для добавления сообщения в UI чат
     * @param sender имя отправителя
     * @param message текст сообщения
     */
    public void addChatMessage(String sender, String message) {
        Platform.runLater(() ->
                chatListView.getItems().add(sender + ": " + message)
        );
    }

    /**
     * Метод для обновления состояния игры
     * @param jsonState состояние игры в формате JSON
     */
    public void updateGameState(String jsonState) {
        if (gameBoard != null) {
            gameBoard.updateState(jsonState); // Обновляем состояние игрового поля
        }

        // Обновляем состояние игры (начата / не начата)
        setGameStarted(jsonState.contains("\"gameStarted\":true"));

        // Разбираем JSON для получения текущего игрока и очков
        Pattern pPlayer = Pattern.compile("\"currentPlayer\":\"(.*?)\"");
        Matcher mPlayer = pPlayer.matcher(jsonState);
        if (mPlayer.find()) {
            this.currentPlayerName = mPlayer.group(1);
        }

        Pattern pScores = Pattern.compile("\"scores\":\\{(.*?)}", Pattern.DOTALL);
        Matcher mScores = pScores.matcher(jsonState);
        if (mScores.find()) {
            Map<String, Integer> scores = getScores(mScores);
            updatePlayerList(scores);
        }
    }

    /**
     * Метод для получения очков игроков из JSON
     * @param mScores матчер для поиска очков
     * @return Map с именами игроков и их очками
     */
    private static Map<String, Integer> getScores(Matcher mScores) {
        String content = mScores.group(1);
        Map<String, Integer> scores = new HashMap<>();

        if (!content.trim().isEmpty()) {
            // Разбиваем строку на пары "имя:очки"
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                // Разбиваем пару на имя и очки
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String name = kv[0].trim().replace("\"", "");
                    try {
                        scores.put(name, Integer.parseInt(kv[1].trim()));
                    } catch (Exception ignored) {}
                }
            }
        }
        return scores;
    }

    /**
     * Метод для обновления списка игроков
     * @param scores Map с именами игроков и их очками
     */
    private void updatePlayerList(Map<String, Integer> scores) {
        Platform.runLater(() -> {
            playerListContainer.getChildren().clear(); // Очищаем список

            // Обрабатываем каждого игрока
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                String name = entry.getKey();
                int score = entry.getValue();
                boolean isActive = name.equals(currentPlayerName);

                HBox playerRow = new HBox(10);
                playerRow.setAlignment(Pos.CENTER_LEFT);
                playerRow.setPadding(new Insets(8));

                playerRow.getStyleClass().add("player-row");
                if (isActive && isGameStarted) {
                    playerRow.getStyleClass().add("player-row-active");
                }

                // Создаем аватар игрока
                PlayerAvatar avatar = new PlayerAvatar(name, isActive && isGameStarted);

                // Создаем метку с именем
                Label nameLabel = new Label(name);
                nameLabel.getStyleClass().add("player-name");

                playerRow.getChildren().addAll(avatar, nameLabel);

                // Если игра начата, добавляем метку с очками игрока и помечаем активного игрока
                if (isGameStarted) {
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label scoreVal = new Label(String.valueOf(score));
                    scoreVal.getStyleClass().add("player-score");

                    playerRow.getChildren().addAll(spacer, scoreVal);
                }

                playerListContainer.getChildren().add(playerRow);
            }
        });
    }

    /**
     * Метод для установки состояния игры
     * @param started true, если игра начата
     */
    public void setGameStarted(boolean started) {
        this.isGameStarted = started;
        // Убираем кнопку "Начать игру" из UI
        Platform.runLater(() -> {
            startButton.setVisible(!started);
            startButton.setManaged(!started);
        });
    }

    /**
     * Метод для сброса игры
     */
    public void resetGame() {
        setGameStarted(false); // Сбрасываем состояние игры
        if (gameBoard != null) {
            gameBoard.reset(); // Сбрасываем игровое поле
        }
    }
}
