package server;

import model.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Обработчик подключений и сетевых событий.
 */
public class ConnectionHandler {
    private Selector selector;
    private ServerSocketChannel serverChannel;

    private final MessageRouter messageRouter;
    private final GameSessionManager sessionManager;
    private final PlayerManager playerManager;
    private final BroadcastService broadcastService;

    /**
     * Конструктор обработчика подключений.
     *
     * @param messageRouter   маршрутизатор сообщений
     * @param sessionManager  менеджер игровых сессий
     * @param playerManager   менеджер игроков
     * @param broadcastService сервис рассылки сообщений
     */
    public ConnectionHandler(MessageRouter messageRouter, GameSessionManager sessionManager,
                             PlayerManager playerManager, BroadcastService broadcastService) {
        this.messageRouter = messageRouter;
        this.sessionManager = sessionManager;
        this.playerManager = playerManager;
        this.broadcastService = broadcastService;
    }

    /**
     * Инициализирует серверный сокет и селектор.
     *
     * @param port порт для прослушивания
     * @throws IOException если возникает ошибка ввода-вывода
     */
    public void initialize(int port) throws IOException {
        // Создаем селектор для обработки сетевых событий
        selector = Selector.open();
        // Создаем серверный канал и привязываем к порту
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        // Регистрируем серверный канал в селекторе для обработки событий подключения
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * Основной цикл обработки событий.
     * Бесконечно ожидает сетевые события и обрабатывает их.
     *
     * @throws IOException если возникает ошибка ввода-вывода (обрыв соединения)
     */
    public void runEventLoop() throws IOException {
        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (!key.isValid()) continue;

                try {
                    handleSelectionKey(key);
                } catch (IOException e) {
                    sessionManager.handlePlayerDisconnect((SocketChannel) key.channel());
                }
            }
        }
    }

    /**
     * Обрабатывает событие на ключе селектора.
     *
     * @param key ключ селектора с событием
     * @throws IOException если возникает ошибка ввода-вывода
     */
    private void handleSelectionKey(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            // Новое подключение
            handleNewConnection(key);
        } else if (key.isReadable()) {
            // Данные от клиента
            handleClientData(key);
        }
    }

    /**
     * Обрабатывает новое подключение клиента.
     * Проверяет лимит игроков и регистрирует клиента для чтения.
     *
     * @param key ключ селектора серверного канала
     * @throws IOException если возникает ошибка ввода-вывода
     */
    private void handleNewConnection(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);

        if (playerManager.getPlayerCount() >= sessionManager.getMaxPlayers()) {
            broadcastService.sendMessageToClient(client, Protocol.encode(
                    Protocol.TYPE_ERROR,
                    "Достигнуто максимальное количество игроков (" + sessionManager.getMaxPlayers() + ")"
            ));
            client.close();
            return;
        }

        client.register(selector, SelectionKey.OP_READ);
    }

    /**
     * Обрабатывает данные от клиента.
     * Читает данные из канала и передает их маршрутизатору сообщений.
     *
     * @param key ключ селектора клиентского канала
     * @throws IOException если возникает ошибка ввода-вывода
     */
    private void handleClientData(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(2048);

        // Читаем данные от клиента
        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            sessionManager.handlePlayerDisconnect(client);
            return;
        }

        // Преобразуем байты в строку
        String rawData = new String(buffer.array(), 0, bytesRead, StandardCharsets.UTF_8);
        // Используем Scanner для разделения на строки (сообщения разделены \n)
        try (Scanner scanner = new Scanner(rawData)) {
            while (scanner.hasNextLine()) {
                String message = scanner.nextLine().trim();
                if (!message.isEmpty()) {
                    // Передаем каждое сообщение маршрутизатору для обработки
                    messageRouter.processMessage(client, message);
                }
            }
        }
    }
}