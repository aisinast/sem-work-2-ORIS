package client;

import javafx.application.Platform;
import model.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static view.GameDialogs.showErrorDialog;

/**
 * Класс клиента для подключения к серверу
 */
public class GameClient {

    private SocketChannel socketChannel;
    private ByteBuffer readBuffer;
    private volatile boolean isRunning = true;

    private GameClientListener listener;

    public void setListener(GameClientListener listener) {
        this.listener = listener;
    }

    public GameClientListener getListener() {
        return listener;
    }

    /**
     * Метод для подключения к серверу
     * @param domain домен сервера
     * @param port порт сервера
     * @return true, если подключение успешно, иначе false
     */
    public boolean connect(String domain, int port) {
        try {
            // Пробуем подключиться к серверу
            this.socketChannel = SocketChannel.open();
            this.socketChannel.configureBlocking(false);
            this.socketChannel.connect(new InetSocketAddress(domain, port));

            // Ждем, пока клиент не подключится к серверу или пока меньше 200 попыток
            int attempts = 0;
            while (!socketChannel.finishConnect()) {
                // noinspection BusyWait
                Thread.sleep(50);
                attempts++;
                if (attempts > 200) {
                    throw new IOException("Unable to connect to server");
                }
            }

            this.readBuffer = ByteBuffer.allocate(64 * 1024);
            this.isRunning = true;

            startReading();
            return true;
        } catch (IOException | InterruptedException e) {
            Platform.runLater(() -> showErrorDialog("Не удалось подключиться к серверу, попробуйте еще раз"));
            System.out.println("Ошибка при подключении к серверу: " + e.getMessage());
            return false;
        }
    }

    /**
     * Метод для чтения сообщений с сервера
     */
    private void startReading() {
        Thread readThread = new Thread(() -> {
            while (isRunning && socketChannel != null && socketChannel.isOpen()) {
                try {
                    int n = socketChannel.read(readBuffer);

                    // Если прочитано -1, значит, сервер разорвал соединение -> отключаемся
                    if (n == -1) {
                        disconnect();
                        break;
                    }

                    // Если прочитано 0, значит, ничего не прочитано -> ждём
                    if (n == 0) {
                        // noinspection BusyWait
                        Thread.sleep(200);
                        continue;
                    }

                    readBuffer.flip(); // Переводим буфер в режим чтения
                    processIncomingMessages(readBuffer);
                    readBuffer.compact(); // Очищаем буфер для следующего чтения

                } catch (IOException | InterruptedException e) {
                    if (isRunning) {
                        Platform.runLater(() -> showErrorDialog("Произошла непредвиденная ошибка при чтении данных :("));
                        System.out.println("Ошибка при чтении данных: " + e.getMessage());
                    }
                    break;
                }
            }
        });

        readThread.setDaemon(true);
        readThread.start();
    }

    /**
     * Метод для отправки сообщений
     * @param message сообщение
     */
    private void sendMessage(String message) {
        if  (socketChannel != null && socketChannel.isConnected()) {
            try {
                socketChannel.write(ByteBuffer.wrap((message + "\n").getBytes()));
            } catch (IOException e) {
                System.out.println("Ошибка при отправке сообщения: " + e.getMessage());
                disconnect();
            }
        }
    }

    /**
     * Метод для отправки сообщения о подключении к игре по протоколу
     * @param message текст сообщения
     */
    public void sendConnectMessage(String message) {
        sendMessage(Protocol.encode(Protocol.TYPE_CONNECT, message));
    }

    /**
     * Метод для отправки сообщения о начале игры по протоколу
     */
    public void sendStartGameMessage() {
        sendMessage(Protocol.encode(Protocol.TYPE_GAME_RESET));
        sendMessage(Protocol.encode(Protocol.TYPE_START_GAME));
    }

    /**
     * Метод для отправки сообщения об открытии карты по протоколу
     * @param message текст сообщения
     */
    public void sendOpenCardMessage(String message) {
        sendMessage(Protocol.encode(Protocol.TYPE_CARD_OPEN, message));
    }

    /**
     * Метод для отправки сообщения в чате по протоколу
     * @param message текст сообщения
     */
    public void sendChatMessage(String message) {
        sendMessage(Protocol.encode(Protocol.TYPE_CHAT_MESSAGE, message));
    }

    /**
     * Метод для обработки входящих сообщений
     * @param readBuffer буфер для чтения сообщений
     */
    private void processIncomingMessages(ByteBuffer readBuffer) {
        while (readBuffer.hasRemaining()) {
            readBuffer.mark(); // Метка для возврата к началу буфера
            boolean lineFound = false;

            // Поиск конца строки
            while (readBuffer.hasRemaining()) {
                byte b = readBuffer.get();
                if (b == '\n') {
                    lineFound = true;
                    break;
                }
            }

            // Если строка не найдена, возвращаемся к началу буфера
            if (!lineFound) {
                readBuffer.reset();
                break;
            }

            int endPos = readBuffer.position(); // Позиция конца строки
            readBuffer.reset(); // Возвращаемся к началу буфера

            int len = endPos - readBuffer.position() - 1; // Длина строки без \n
            byte[] bytes = new byte[len];
            readBuffer.get(bytes);
            readBuffer.get(); // Пропускаем \n

            String message = new String(bytes, StandardCharsets.UTF_8);
            if (!message.isEmpty()) {
                handleMessage(message);
            }
        }
    }

    /**
     * Метод для обработки сообщений по протоколу
     * @param message сообщение в виде строки
     */
    private void handleMessage(String message) {
        String[] parts = Protocol.decode(message);
        if (parts.length == 0) return;

        // Получаем тип сообщения
        int type;
        try {
            type = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            System.out.println("Неверный формат сообщения: " + message);
            return;
        }

        // Обрабатываем сообщение в зависимости от типа и передаем слушателю в соответствующий метод
        switch (type) {
            case Protocol.TYPE_GAME_STATE:
                if (parts.length > 1) {
                    String jsonState = parts[1];

                    if (listener != null) {
                        listener.onGameStateUpdate(jsonState);
                    }
                }
                break;
            case Protocol.TYPE_CHAT_MESSAGE:
                if (parts.length > 2) {
                    String sender = parts[1];
                    String text = parts[2];
                    if (listener != null) {
                        listener.onChatMessage(sender, text);
                    }
                }
                break;
            case Protocol.TYPE_ERROR:
                if (parts.length > 1) {
                    String errorMsg = parts[1];
                    if (listener != null) {
                        listener.onError(errorMsg);
                    } else {
                        System.err.println("Server Error: " + errorMsg);
                    }
                }
                break;
            case Protocol.TYPE_GAME_OVER:
                if (parts.length > 2) {
                    String winner = parts[1];
                    int score = Integer.parseInt(parts[2]);
                    if (listener != null) {
                        listener.onGameOver(winner, score);
                    }
                }
                break;
            case Protocol.TYPE_SYSTEM:
                if (parts.length > 2) {
                    String sysText = parts[2]; // parts[1] это "Система", можно игнорировать
                    if (listener != null) {
                        listener.onSystemMessage(sysText);
                    }
                }
                break;
            case Protocol.TYPE_GAME_RESET:
                if (parts.length > 2) {
                    listener.onGameReset();
                }
                break;
        }
    }

    /**
     * Метод для отключения от сервера
     */
    public void disconnect() {
        isRunning = false;

        try {
            if (socketChannel != null) {
                socketChannel.close();
            }
        } catch (IOException ignored) {}

        System.out.println("Отключен от сервера");
    }
}
