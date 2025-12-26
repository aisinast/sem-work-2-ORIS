package server;

import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Менеджер игроков.
 * Управляет информацией о подключенных игроках и их именах.
 * Поддерживает порядок подключения игроков.
 */
public class PlayerManager {
    // Канал игрока -> его имя
    private final Map<SocketChannel, String> playerNames = new HashMap<>();
    // Список подключенных игроков
    private final List<SocketChannel> players = new ArrayList<>();

    /**
     * Добавляет нового игрока.
     *
     * @param client канал игрока
     * @param name имя игрока
     */
    public void addPlayer(SocketChannel client, String name) {
        playerNames.put(client, name);
        if (!players.contains(client)) {
            players.add(client);
        }
    }

    /**
     * Удаляет игрока.
     *
     * @param client канал игрока
     * @return имя удаленного игрока или null, если игрок не найден
     */
    public String removePlayer(SocketChannel client) {
        players.remove(client);
        return playerNames.remove(client);
    }

    /**
     * Возвращает имя игрока по его каналу.
     *
     * @param client канал игрока
     * @return имя игрока или "Unknown", если игрок не найден
     */
    public String getPlayerName(SocketChannel client) {
        return playerNames.getOrDefault(client, "Unknown");
    }

    /**
     * Возвращает список всех подключенных игроков.
     *
     * @return список каналов игроков
     */
    public List<SocketChannel> getAllPlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Возвращает список имен всех подключенных игроков.
     * Сохраняет порядок подключения.
     *
     * @return список имен игроков
     */
    public List<String> getPlayerNamesList() {
        List<String> names = new ArrayList<>();
        for (SocketChannel player : players) {
            names.add(playerNames.get(player));
        }
        return names;
    }

    /**
     * Возвращает количество подключенных игроков.
     *
     * @return количество игроков
     */
    public int getPlayerCount() {
        return players.size();
    }
}