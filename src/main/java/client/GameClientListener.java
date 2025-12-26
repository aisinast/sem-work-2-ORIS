package client;

import javafx.application.Platform;
import view.GameView;

import static javafx.application.Platform.runLater;
import static view.GameDialogs.*;

/**
 * Слушатель событий от сервера
 */
public class GameClientListener {

    private static GameView gameView;

    public void setGameView(GameView gameView) {
        GameClientListener.gameView = gameView;
    }

    /**
     * Обновление состояния игры
     * @param jsonState состояние игры в формате JSON, передается от сервера
     */
    public void onGameStateUpdate(String jsonState) {
        if (gameView != null) {
            gameView.updateGameState(jsonState);
        }
    }

    /**
     * Отображение сообщения от игрока в чате
     * @param sender имя игрока, отправившего сообщение
     * @param message текст сообщения от игрока
     */
    public void onChatMessage(String sender, String message) {
        if (gameView != null) {
            gameView.addChatMessage(sender, message);
        }
    }

    /**
     * Отображение системного сообщения в диалоге. Отображается только сообщение о выходе игрока из игры, остальные
     * выводятся в консоль
     * @param message текст системного сообщения от сервера
     */
    public void onSystemMessage(String message) {
        if (message.contains("покинул игру")) {
            // Обрабатываем сообщение в отдельном FX потоке
            runLater(() -> {
                showInfoDialog(message); // Отображаем диалоговое окно с сообщением

                if (gameView != null) {
                    gameView.resetGame(); // Сбрасываем игру
                }
            });
        }
    }

    /**
     * Отображение ошибки в диалоге
     * @param errorMessage текст ошибки от сервера
     */
    public void onError(String errorMessage) {
        // Обрабатываем ошибку в отдельном FX потоке
        runLater(() -> {
            showErrorDialog(errorMessage); // Отображаем диалоговое окно с ошибкой
        });
    }

    /**
     * Отображение диалога с результатами игры (победители и очки)
     * @param winners имена победителей
     * @param score количество очков победителей
     */
    public void onGameOver(String winners, int score) {
        // Обрабатываем результат в отдельном FX потоке
        runLater(() -> {
            showGameOverDialog(winners, score); // Отображаем диалоговое окно с результатами игры

            if (gameView != null) {
                gameView.resetGame(); // Сбрасываем игру после отображения результата
            }
        });
    }

    /**
     * Сброс игры после завершения игры / при ошибке
     */
    public void onGameReset() {
        // Обрабатываем сброс игры в отдельном FX потоке
        Platform.runLater(() -> {
            gameView.resetGame(); // Сбрасываем игру
        });
    }
}
