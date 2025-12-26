package server;

import model.GameModel;
import model.Protocol;

import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Менеджер игровых сессий.
 * Управляет логикой игры: подключение игроков, начало игры, ходы, завершение.
 */
public class GameSessionManager {
    private static final int MAX_PLAYERS = 4;
    private static final int MIN_PLAYERS = 2;

    private final GameModel gameModel;
    private final PlayerManager playerManager;
    private final BroadcastService broadcastService;

    /**
     * Конструктор менеджера игровых сессий.
     *
     * @param playerManager менеджер игроков
     * @param broadcastService сервис рассылки сообщений
     */
    public GameSessionManager(PlayerManager playerManager, BroadcastService broadcastService) {
        this.playerManager = playerManager;
        this.broadcastService = broadcastService;
        this.gameModel = new GameModel();
    }

    /**
     * Обрабатывает подключение нового игрока.
     *
     * @param client канал подключенного клиента
     * @param playerName имя игрока
     */
    public void handlePlayerConnect(SocketChannel client, String playerName) {
        // Проверяем, не началась ли игра
        if (gameModel.isGameStarted()) {
            broadcastService.sendMessageToClient(client, Protocol.encode(
                    Protocol.TYPE_ERROR,
                    "Игра уже началась. Присоединиться нельзя"
            ));
            return;
        }

        // Добавляем игрока в менеджер и в модель игры
        playerManager.addPlayer(client, playerName);
        gameModel.addPlayer(playerName);

        // Отправляем текущее состояние игры
        broadcastGameState();

        System.out.println("Игрок " + playerName + " подключился. Всего игроков: " + playerManager.getPlayerCount());
    }

    /**
     * Обрабатывает запрос на начало игры.
     *
     * @param client канал клиента, отправившего запрос
     */
    public void handleStartGame(SocketChannel client) {
        // Проверяем минимальное количество игроков
        if (playerManager.getPlayerCount() < MIN_PLAYERS) {
            broadcastService.sendMessageToClient(client, Protocol.encode(
                    Protocol.TYPE_ERROR,
                    "Для начала игры нужно минимум " + MIN_PLAYERS + " игрока"
            ));
            return;
        }

        // Устанавливаем флаг начала игры
        gameModel.setGameStarted(true);

        // Устанавливаем очередь ходов на основе списка игроков
        gameModel.nextPlayer(playerManager.getPlayerNamesList());

        broadcastGameState();
    }

    /**
     * Обрабатывает открытие карточки игроком.
     *
     * @param client канал клиента
     * @param playerName имя игрока
     * @param cardPositionStr позиция карточки в виде строки
     */
    public void handleCardOpen(SocketChannel client, String playerName, String cardPositionStr) {
        // Проверяем, начата ли игра и не завершена ли она
        if (!gameModel.isGameStarted() || gameModel.isGameOver()) {
            return;
        }

        // Проверяем, что ход текущего игрока
        if (!playerName.equals(gameModel.getCurrentPlayer())) {
            broadcastService.sendMessageToClient(client, Protocol.encode(
                    Protocol.TYPE_ERROR,
                    "Сейчас не ваш ход"
            ));
            return;
        }

        try {
            int cardPosition = Integer.parseInt(cardPositionStr);

            // Пытаемся открыть карточку в модели игры
            if (!gameModel.openCard(cardPosition)) {
                broadcastService.sendMessageToClient(client, Protocol.encode(
                        Protocol.TYPE_ERROR,
                        "Невозможно открыть карточку"
                ));
                return;
            }

            broadcastGameState();

            // Если открыто две карточки, проверяем совпадение
            if (gameModel.getOpenedCards().size() == 2) {
                checkMatch();
            }

        } catch (NumberFormatException e) {
            System.out.println("Неверный формат позиции карточки");
        }
    }

    /**
     * Проверяет, совпадают ли открытые карточки.
     * Обновляет счет и передает ход при необходимости.
     */
    private void checkMatch() {
        boolean match = gameModel.checkMatch();

        if (!match) {
            // Если карточки не совпали, передаем ход следующему игроку
            gameModel.nextPlayer(playerManager.getPlayerNamesList());

            String nextTurnMessage = Protocol.encode(
                    Protocol.TYPE_PLAYER_TURN,
                    gameModel.getCurrentPlayer()
            );
            broadcastService.broadcastToAll(nextTurnMessage);
        }

        // Очищаем открытые карточки через 2 секунды
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                gameModel.clearOpenedCards();
                broadcastGameState();

                if (gameModel.isGameOver()) {
                    handleGameOver();
                }
            }
        }, 2000);
    }

    /**
     * Метод обрабатывает сброс игры.
     * Возвращает игру в исходное состояние.
     */
    public void handleGameReset() {
        gameModel.resetGame();
        broadcastGameState();

        System.out.println("Игра сброшена. Ожидание игроков...");
    }

    /**
     * Обрабатывает завершение игры.
     * Определяет победителей и сбрасывает игру.
     */
    private void handleGameOver() {
        Map<String, Integer> scores = gameModel.getPlayerScores();
        StringBuilder winners = new StringBuilder();
        int maxScore = scores.values().stream().max(Integer::compareTo).orElse(0);

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() == maxScore) {
                winners.append(entry.getKey()).append(";");
            }
        }

        gameModel.resetGame();

        String gameOverMessage = Protocol.encode(
                Protocol.TYPE_GAME_OVER,
                winners.toString(),
                String.valueOf(maxScore)
        );
        broadcastService.broadcastToAll(gameOverMessage);
    }

    /**
     * Обрабатывает отключение игрока.
     * Удаляет игрока из всех структур и при необходимости сбрасывает игру.
     *
     * @param client канал отключившегося клиента
     */
    public void handlePlayerDisconnect(SocketChannel client) {
        String playerName = playerManager.removePlayer(client);

        if (playerName != null) {
            String message = Protocol.encode(
                    Protocol.TYPE_SYSTEM,
                    Protocol.SYSTEM_USER,
                    playerName + " покинул игру"
            );
            broadcastService.broadcastToAll(message);

            gameModel.removePlayer(playerName);
        }

        try {
            client.close();
        } catch (Exception ignored) {}

        // Если игра началась и игрок отключился - сбрасываем игру
        if (gameModel.isGameStarted() && !gameModel.isGameOver()) {
            handleGameReset();
        }

        broadcastGameState();

        System.out.println("Игрок " + (playerName != null ? playerName : "Аноним") +
                " отключился. Осталось игроков: " + playerManager.getPlayerCount());
    }

    /**
     * Рассылает текущее состояние игры всем игрокам.
     */
    private void broadcastGameState() {
        broadcastService.broadcastGameState(gameModel, MAX_PLAYERS, MIN_PLAYERS);
    }

    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }
}