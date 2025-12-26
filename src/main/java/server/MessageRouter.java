package server;

import model.Protocol;
import java.nio.channels.SocketChannel;

/**
 * Маршрутизатор сообщений.
 * Определяет тип входящего сообщения и направляет его соответствующему обработчику.
 */
public class MessageRouter {
    private final GameSessionManager sessionManager;
    private final PlayerManager playerManager;
    private final BroadcastService broadcastService;

    /**
     * Конструктор маршрутизатора сообщений.
     *
     * @param sessionManager менеджер игровых сессий
     * @param playerManager менеджер игроков
     * @param broadcastService сервис рассылки сообщений
     */
    public MessageRouter(GameSessionManager sessionManager, PlayerManager playerManager, BroadcastService broadcastService) {
        this.sessionManager = sessionManager;
        this.playerManager = playerManager;
        this.broadcastService = broadcastService;
    }

    /**
     * Обрабатывает входящее сообщение от клиента.
     * Декодирует сообщение и направляет его соответствующему обработчику.
     *
     * @param client канал клиента
     * @param rawMessage сырое сообщение в формате протокола
     */
    public void processMessage(SocketChannel client, String rawMessage) {
        // Декодируем сообщение по протоколу
        String[] parts = Protocol.decode(rawMessage);
        if (parts.length == 0) return;

        // Определяем тип сообщения
        int messageType = parseMessageType(parts[0]);
        if (messageType == -1) return;

        // Получаем имя игрока для идентификации
        String playerName = playerManager.getPlayerName(client);

        switch (messageType) {
            // Обработка подключения нового игрока
            case Protocol.TYPE_CONNECT:
                if (parts.length > 1) {
                    sessionManager.handlePlayerConnect(client, parts[1]);
                }
                break;

            // Обработка запроса на начало игры
            case Protocol.TYPE_START_GAME:
                sessionManager.handleStartGame(client);
                break;

            // Обработка открытия карточки
            case Protocol.TYPE_CARD_OPEN:
                if (parts.length > 1) {
                    sessionManager.handleCardOpen(client, playerName, parts[1]);
                }
                break;

            // Обработка сообщения в чат
            case Protocol.TYPE_CHAT_MESSAGE:
                if (parts.length > 1) {
                    String message = parts[1];
                    String chatPacket = Protocol.encode(
                            Protocol.TYPE_CHAT_MESSAGE,
                            playerName,
                            message
                    );
                    broadcastService.broadcastToAll(chatPacket);
                }
                break;

            // Обработка запроса на сброс игры
            case Protocol.TYPE_GAME_RESET:
                sessionManager.handleGameReset();
                break;
        }
    }

    /**
     * Парсит тип сообщения из строки.
     *
     * @param typeStr строка с типом сообщения
     * @return числовой тип сообщения или -1 при ошибке
     */
    private int parseMessageType(String typeStr) {
        try {
            return Integer.parseInt(typeStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}