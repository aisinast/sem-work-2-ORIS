package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

/**
 * Класс для отображения аватара игрока в панели игроков рядом с именем (и обводкой, если сейчас ход этого игрока)
 */
public class PlayerAvatar extends Canvas {

    private final String playerName;
    private final boolean isActive;

    public PlayerAvatar(String playerName, boolean isActive) {
        super(40, 40); // Размер холста
        this.playerName = playerName;
        this.isActive = isActive;
        draw();
    }

    /**
     * Метод для рисования аватара игрока
     */
    private void draw() {
        // Получаем контекст для рисования
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        double cx = w / 2;
        double cy = h / 2;
        double radius = Math.min(w, h) - 4; // Отступ 2 пикселя с каждой стороны

        gc.clearRect(0, 0, w, h); // Очищаем холст перед рисованием

        // Цвет активного круга - синий, неактивного - серый
        Color circleColor = isActive ? Color.web("#3498db") : Color.web("#7f8c8d");

        // Рисуем тень
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(3, 3, radius, radius);

        // Рисуем основной круг
        gc.setFill(circleColor);
        gc.fillOval(1, 1, radius, radius);

        // Рисуем белый контур
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(1, 1, radius, radius);

        // Рисуем обводку у активного игрока
        if (isActive) {
            gc.setStroke(Color.web("#f1c40f"));
            gc.setLineWidth(2);
            // Рисуем чуть больше основного круга
            gc.strokeOval(0, 0, w, h);
        }

        // Рисуем первую букву имени игрока
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        String letter = (playerName != null && !playerName.isEmpty())
                ? playerName.substring(0, 1).toUpperCase()
                : "?";

        // Рисуем букву в центре
        gc.fillText(letter, cx, cy + 1);
    }
}
