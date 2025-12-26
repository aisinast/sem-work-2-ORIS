package util;

import client.GameClient;
import client.GameClientListener;
import javafx.stage.Stage;
import model.ConnectionInfo;
import server.GameServer;
import view.GameView;

import static com.sun.javafx.application.PlatformImpl.runLater;
import static view.GameDialogs.showErrorDialog;

/**
 * Класс-помощник для подключения клиента и сервера
 */
public class ConnectionHelper {

    /**
     * Метод для запуска сервера
     * @param connectionInfo информация о подключении (домен, порт, имя пользователя)
     */
    public static void startServer(ConnectionInfo connectionInfo) {
        String domain = connectionInfo.domain();
        int port = connectionInfo.port();

        if (domain == null || domain.isEmpty()) {
            showErrorDialog("Домен не может быть пустым");
        }

        if (port <= 0 || port > 65535) {
            showErrorDialog("Порт должен быть в диапазоне от 1 до 65535");
        }

        // Запуск сервера в отдельном потоке
        Thread serverThread = getServerThread(port);
        serverThread.start();

        // Ждем 150 мс, чтобы сервер успел запуститься
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        connectClient(connectionInfo);
    }

    /**
     * Метод для запуска сервера в отдельном потоке
     * @param port порт сервера
     * @return поток сервера
     */
    private static Thread getServerThread(int port) {
        Thread serverThread = new Thread(() -> {
            try {
                new GameServer().start(port);
            } catch (Exception e) {
                runLater(() ->
                        showErrorDialog("Произошла неизвестная ошибка при запуске сервера, попробуйте еще раз")
                );
                System.out.println("Ошибка при запуске сервера: " + e.getMessage());
            }
        });

        serverThread.setDaemon(true); // Демонический поток для автоматического завершения при закрытии приложения
        return serverThread;
    }

    /**
     * Метод для подключения клиента к серверу
     * @param connectionInfo информация о подключении (домен, порт, имя пользователя)
     */
    public static void connectClient(ConnectionInfo connectionInfo) {

        String domain = connectionInfo.domain();
        int port = connectionInfo.port();
        String username = connectionInfo.username();

        GameClient client = new GameClient();
        GameClientListener clientListener = new GameClientListener();

        client.setListener(clientListener);

        boolean success = client.connect(domain, port);

        if (success) {
            System.out.println("Клиент подключен");
            client.sendConnectMessage(username);

            GameView gameView = new GameView(client);
            client.getListener().setGameView(gameView);

            gameView.start(new Stage());
        } else {
            showErrorDialog("Не удалось подключиться к игре");
        }
    }
}
