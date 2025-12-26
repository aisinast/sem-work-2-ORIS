package model;

import java.util.*;

/**
 * Модель игры "Мемо".
 * Управляет состоянием игрового поля, карточками, счетами игроков и логикой игры.
 * Игра использует поле 6x6 с 36 карточками (18 пар).
 */
public class GameModel {
    private static final int BOARD_SIZE = 6;

    /**
     * Двумерный массив для хранения состояния каждой карточки на поле.
     * Возможные значения:
     * - "hidden" (скрыта) - карточка закрыта, значение неизвестно игроку
     * - "opened" (открыта) - карточка открыта в текущем ходе, значение видно
     * - "matched" (найдена пара) - карточка была успешно сопоставлена с парной
     */
    private String[][] board;

    /**
     * Карта для хранения значений карточек.
     * Ключ: позиция на поле (0-35)
     * Значение: числовое значение карточки (1-18)
     * Каждое значение встречается дважды (пары карточек).
     */
    private Map<Integer, Integer> cardValues;

    // Список для хранения позиций карточек, открытых в текущем ходе
    private List<Integer> openedCards;

    /**
     * Карта для хранения счетов игроков.
     * Ключ: имя игрока
     * Значение: количество найденных пар
     */
    private Map<String, Integer> playerScores;

    // Текущий игрок, который имеет право делать ход
    private String currentPlayer;

    // Флаг, указывающий, начата ли игра
    private boolean gameStarted;

    // Флаг, указывающий, завершена ли игра
    private boolean gameOver;

    public GameModel() {
        initializeBoard();
        this.openedCards = new ArrayList<>();
        this.playerScores = new HashMap<>();
        this.gameStarted = false;
        this.gameOver = false;
    }

    /**
     * Инициализирует игровое поле.
     * Создает поле 6x6, генерирует пары карточек и случайным образом размещает их на поле.
     * Все карточки устанавливаются в состояние "hidden" (скрыты).
     */
    private void initializeBoard() {
        board = new String[BOARD_SIZE][BOARD_SIZE];
        cardValues = new HashMap<>();

        List<Integer> values = new ArrayList<>();
        for (int i = 1; i <= 18; i++) {
            values.add(i);
            values.add(i);
        }

        // Перемешиваем значения для случайного распределения карточек по полю
        Collections.shuffle(values);

        int index = 0;
        // Заполняем игровое поле
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                int cardValue = values.get(index++);
                board[row][col] = "hidden";
                cardValues.put(row * BOARD_SIZE + col, cardValue);
            }
        }
    }

    /**
     * Открывает карточку на указанной позиции.
     *
     * @param position позиция карточки на поле (0-35)
     * @return true - карточка успешно открыта, false - карточку открыть нельзя
     */
    public boolean openCard(int position) {
        if (openedCards.size() >= 2 || gameOver) {
            return false;
        }

        openedCards.add(position);
        return true;
    }

    /**
     * Проверяет совпадение открытых карточек.
     *
     * @return true - карточки совпали, false - карточки разные
     */
    public boolean checkMatch() {
        if (openedCards.size() != 2) {
            return false;
        }

        int pos1 = openedCards.get(0);
        int pos2 = openedCards.get(1);

        boolean match = cardValues.get(pos1).equals(cardValues.get(pos2));

        // Если карточки совпали
        if (match) {
            // Вычисляем координаты первой карточки на поле
            int row1 = pos1 / BOARD_SIZE;
            int col1 = pos1 % BOARD_SIZE;

            // Вычисляем координаты второй карточки на поле
            int row2 = pos2 / BOARD_SIZE;
            int col2 = pos2 % BOARD_SIZE;

            // Обновляем состояние карточек на поле - отмечаем как "найденную пару"
            board[row1][col1] = "matched";
            board[row2][col2] = "matched";

            // Начисляем очки текущему игроку
            playerScores.put(currentPlayer,
                    playerScores.getOrDefault(currentPlayer, 0) + 1);

            // Проверяем, не завершилась ли игра
            checkGameOver();
        }

        return match;
    }

    /**
     * Очищает список открытых карточек.
     * Вызывается после проверки совпадения (через 2 секунды) для подготовки к следующему ходу.
     */
    public void clearOpenedCards() {
        openedCards.clear();
    }

    /**
     * Передает ход следующему игроку по списку игроков в порядке подключения.
     *
     * @param players список имен игроков в порядке подключения
     */
    public void nextPlayer(List<String> players) {
        if (players.isEmpty()) return;

        if (currentPlayer == null) {
            currentPlayer = players.getFirst();
            return;
        }

        // Находим индекс текущего игрока в списке
        int currentIndex = players.indexOf(currentPlayer);
        // Вычисляем индекс следующего игрока
        currentPlayer = players.get((currentIndex + 1) % players.size());
    }

    /**
     * Удаляет игрока из модели игры.
     *
     * @param playerName имя удаляемого игрока
     */
    public void removePlayer(String playerName) {
        if (!playerScores.isEmpty()) {
            playerScores.remove(playerName);
        }
    }

    /**
     * Проверяет, завершена ли игра.
     * Игра считается завершенной, когда все карточки на поле имеют состояние "matched".
     * Устанавливает флаг gameOver в true при завершении игры.
     */
    private void checkGameOver() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col].equals("hidden")) {
                    return;
                }
            }
        }
        gameOver = true;
    }

    /**
     * Возвращает текущее состояние игры в виде карты.
     *
     * @return Map с полным состоянием игры
     */
    public Map<String, Object> getGameState() {
        Map<String, Object> state = new HashMap<>();
        Map<String, String> boardState = new HashMap<>();

        for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
            int row = i / BOARD_SIZE;
            int col = i % BOARD_SIZE;
            String cardState = board[row][col];

            // Если карточка в списке открытых и еще не была сопоставлена
            if (openedCards.contains(i) && cardState.equals("hidden")) {
                // Помечаем карточку как открытую и показываем ее значение
                boardState.put(String.valueOf(i), "opened_" + cardValues.get(i));
            } else {
                // Иначе сохраняем текущее состояние
                boardState.put(String.valueOf(i), cardState);
            }
        }

        state.put("board", boardState);
        state.put("scores", new HashMap<>(playerScores));
        state.put("currentPlayer", currentPlayer);
        state.put("gameStarted", gameStarted);
        state.put("gameOver", gameOver);
        state.put("openedCards", new ArrayList<>(openedCards));

        return state;
    }

    /**
     * Добавляет нового игрока в игру.
     * Инициализирует счет игрока значением 0.
     *
     * @param playerName имя нового игрока
     */
    public void addPlayer(String playerName) {
        if (!playerScores.containsKey(playerName)) {
            playerScores.put(playerName, 0);
        }
    }

    /**
     * Сбрасывает игру к начальному состоянию.
     * Пересоздает игровое поле, очищает счета и сбрасывает все флаги.
     */
    public void resetGame() {
        initializeBoard();
        openedCards.clear();
        clearScores();
        currentPlayer = null;
        gameStarted = false;
        gameOver = false;
    }

    /**
     * Сбрасывает счета всех игроков к 0.
     */
    private void clearScores() {
        playerScores.replaceAll((_, _) -> 0);
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public List<Integer> getOpenedCards() {
        return openedCards;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public Map<String, Integer> getPlayerScores() {
        return playerScores;
    }
}