package server;

import java.io.IOException;

/**
 * Основной класс игрового сервера.
 * Инициализирует все компоненты сервера и запускает его.
 */
public class GameServer {
    private final ConnectionHandler connectionHandler;

    public GameServer() {
        PlayerManager playerManager = new PlayerManager();
        BroadcastService broadcastService = new BroadcastService(playerManager);
        GameSessionManager sessionManager = new GameSessionManager(playerManager, broadcastService);
        MessageRouter messageRouter = new MessageRouter(sessionManager, playerManager, broadcastService);
        this.connectionHandler = new ConnectionHandler(messageRouter, sessionManager, playerManager, broadcastService);
    }

    /**
     * Запускает игровой сервер на указанном порту.
     *
     * @param port порт для прослушивания
     * @throws IOException если возникает ошибка ввода-вывода при инициализации
     */
    public void start(int port) throws IOException {
        System.out.println("Сервер игры Мемо запущен на порту " + port);
        // Инициализируем обработчик подключений
        connectionHandler.initialize(port);
        // Запускаем основной цикл обработки событий
        connectionHandler.runEventLoop();
    }
}