package model;

/**
 * Рекорд для хранения информации о соединении
 * @param domain домен сервера
 * @param port порт
 * @param username имя пользователя
 */
public record ConnectionInfo(
        String domain,
        int port,
        String username
) {}
