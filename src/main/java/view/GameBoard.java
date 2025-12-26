package view;

import client.GameClient;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для отображения игрового поля
 */
public class GameBoard extends GridPane {

    private static final int ROWS = 6;
    private static final int COLS = 6;
    private static final int CARD_SIZE = 100;

    private final GameClient gameClient;

    // Кэш для картинок, чтобы не грузить их каждый раз с диска
    private final Map<Integer, Image> memeCache = new HashMap<>();

    public GameBoard(GameClient gameClient) {
        setHgap(10);
        setVgap(10);
        setAlignment(Pos.CENTER);

        this.gameClient = gameClient;

        initGrid(gameClient);
    }

    /**
     * Метод для отрисовки игрового поля (для каждой карточки: создаем MemoryCardView, добавляем в GridPane,
     * устанавливаем обработчик клика)
     * @param gameClient клиент игры
     */
    private void initGrid(GameClient gameClient) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                MemoryCardView card = new MemoryCardView(row, col, CARD_SIZE, CARD_SIZE);
                add(card, col, row);

                card.setOnMouseClicked(_ -> {
                    if (card.isClickable() && gameClient != null) {
                        int index = card.getLinearIndex(COLS);
                        gameClient.sendOpenCardMessage(String.valueOf(index));
                    }
                });
            }
        }
    }

    /**
     * Основной метод обновления поля по JSON от сервера
     */
    public void updateState(String jsonState) {
        // Разбор JSON
        Pattern p = Pattern.compile("\"(\\d+)\":\"(opened_(\\d+)|matched|hidden)\"");
        Matcher m = p.matcher(jsonState);

        Map<Integer, String> newStates = new HashMap<>(); // индекс -> состояние (opened, matched, hidden)
        Map<Integer, Integer> newImages = new HashMap<>(); // индекс -> id картинки

        while (m.find()) {
            try {
                int index = Integer.parseInt(m.group(1));
                String fullState = m.group(2);

                // Если открыта - запоминаем ID картинки и добавляем в newImages
                if (fullState.startsWith("opened_")) {
                    newStates.put(index, "opened");
                    int imgId = Integer.parseInt(m.group(3));
                    newImages.put(index, imgId);
                } else {
                    newStates.put(index, fullState); // "matched" или "hidden"
                }
            } catch (Exception e) {
                System.out.println("Произошла ошибка при разборе JSON: " + e.getMessage());
            }
        }

        // Обновляем UI в FX-потоке
        Platform.runLater(() -> {
            for (Node node : getChildren()) {
                if (node instanceof MemoryCardView card) {
                    int idx = card.getLinearIndex(COLS);

                    // Если про карту нет данных - считаем hidden
                    String state = newStates.getOrDefault(idx, "hidden");

                    if ("opened".equals(state)) {
                        // Если открыта - загружаем картинку и показываем
                        if (newImages.containsKey(idx)) {
                            card.setFrontImage(getMemeImage(newImages.get(idx)));
                        }
                        card.showFrontAnimated();
                        card.setDisable(true); // Пока открыта - нельзя кликать
                    }
                    else if ("matched".equals(state)) {
                        // Совпавшая пара - показываем обе карты и делаем не кликабельными
                        card.showFrontAnimated();
                        card.setDisable(true);
                        card.setOpacity(0.5); // Визуально помечаем как "сыгранную" (полупрозрачную)
                    }
                    else {
                        // hidden
                        card.showBackAnimated();
                        card.setDisable(false);
                        card.setOpacity(1.0);
                    }
                }
            }
        });
    }

    /**
     * Метод для сброса игрового поля (очистка всех карт)
     */
    public void reset() {
        Platform.runLater(() -> initGrid(gameClient));
    }

    /**
     * Метод для получения картинки по ID
     * @param id ID картинки (от 1 до 18)
     * @return картинку
     */
    private Image getMemeImage(int id) {
        if (!memeCache.containsKey(id)) {
            try {
                // Путь: src/main/resources/images/memes/1.png ... 18.png
                String path = "/images/memes/" + id + ".png";
                Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
                memeCache.put(id, img);
            } catch (Exception e) {
                System.err.println("Картинка не найдена: " + id);
                return null;
            }
        }
        return memeCache.get(id);
    }
}
