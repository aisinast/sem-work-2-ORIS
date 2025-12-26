package server;

import model.GameModel;
import model.Protocol;
import util.JsonUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Сервис для отправки сообщений игрокам.
 */
public class BroadcastService {
    private final PlayerManager playerManager;

    /**
     * Конструктор сервиса рассылки.
     *
     * @param playerManager менеджер игроков для получения списка подключенных клиентов
     */
    public BroadcastService(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    /**
     * Рассылает сообщение всем подключенным игрокам.
     *
     * @param message сообщение для рассылки
     */
    public void broadcastToAll(String message) {
        for (SocketChannel client : playerManager.getAllPlayers()) {
            sendMessageToClient(client, message);
        }
    }

    /**
     * Отправляет сообщение конкретному клиенту.
     * Если происходит ошибка ввода-вывода, соединение закрывается.
     *
     * @param client  канал клиента
     * @param message сообщение для отправки
     */
    public void sendMessageToClient(SocketChannel client, String message) {
        try {
            if (client.isConnected()) {
                ByteBuffer buffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));
                client.write(buffer);
            }
        } catch (IOException e) {
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Рассылает текущее состояние игры.
     * Формирует JSON с состоянием игры и отправляет его по протоколу.
     *
     * @param gameModel   модель игры для получения состояния
     * @param maxPlayers  максимальное количество игроков
     * @param minPlayers  минимальное количество игроков для начала игры
     */
    public void broadcastGameState(GameModel gameModel, int maxPlayers, int minPlayers) {
        // Получаем текущее состояние игры из модели
        Map<String, Object> state = gameModel.getGameState();

        // Добавляем информацию об игроках и ограничениях по количеству
        state.put("players", playerManager.getPlayerNamesList());
        state.put("maxPlayers", maxPlayers);
        state.put("minPlayers", minPlayers);

        // Преобразуем состояние в JSON строку
        String stateJson = JsonUtil.mapToJson(state);
        // Формируем пакет состояния игры по протоколу
        String gameStatePacket = Protocol.encode(Protocol.TYPE_GAME_STATE, stateJson);
        broadcastToAll(gameStatePacket);
    }
}