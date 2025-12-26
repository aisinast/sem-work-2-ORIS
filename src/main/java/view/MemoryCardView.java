package view;

import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;

import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Класс, отрисовывающий карточку игры
 */
public class MemoryCardView extends StackPane {

    private final Rectangle back;
    private final int row;
    private final int col;
    private final ImageView front;
    private boolean open;

    // Базовый стиль
    private static final String BASE_STYLE = "-fx-border-color: black; -fx-border-width: 1px; -fx-background-color: lightgray; -fx-opacity: 1.0;";

    public MemoryCardView(int row, int col, double w, double h) {
        this.row = row;
        this.col = col;

        back = new Rectangle(w, h);
        back.setFill(Color.WHITE);
        back.setStroke(Color.BLACK);
        back.setStrokeWidth(1);

        front = new ImageView();
        front.setFitWidth(w);
        front.setFitHeight(h);

        getChildren().addAll(back, front);
        showBack();

        // Устанавливаем базовый стиль при создании
        this.setStyle(BASE_STYLE);
        this.setPrefSize(w, h);
    }

    /**
     * Метод для анимированного переворота карты лицом вверх
     */
    public void showFrontAnimated() {
        if (open) return;

        // Вращаем рубашку
        RotateTransition hideBack = new RotateTransition(Duration.millis(300), back);
        hideBack.setAxis(Rotate.Y_AXIS);
        hideBack.setFromAngle(0);
        hideBack.setToAngle(90);

        // Вращаем лицо
        RotateTransition showFront = new RotateTransition(Duration.millis(300), front);
        showFront.setAxis(Rotate.Y_AXIS);
        showFront.setFromAngle(90);
        showFront.setToAngle(0);

        hideBack.setOnFinished(_ -> {
            back.setVisible(false);
            front.setVisible(true);
        });

        // Создаем последовательность анимаций
        SequentialTransition flip = new SequentialTransition(hideBack, showFront);
        flip.play();
        open = true;
    }

    /**
     * Метод для отображения рубашки карты
     */
    public void showBack() {
        front.setVisible(false);
        back.setVisible(true);
        open = false;
        // Сброс поворотов элементов, если вдруг они остались повернутыми
        back.setRotate(0);
        front.setRotate(0);
    }

    /**
     * Метод для анимированного переворота карты рубашкой вверх
     */
    public void showBackAnimated() {
        if (!open) return;

        // 1. Вращаем лицо от 0 до 90 (скрываем)
        RotateTransition hideFront = new RotateTransition(Duration.millis(300), front);
        hideFront.setAxis(Rotate.Y_AXIS);
        hideFront.setFromAngle(0);
        hideFront.setToAngle(90);

        // 2. Вращаем рубашку от 90 до 0 (показываем)
        RotateTransition showBack = new RotateTransition(Duration.millis(300), back);
        showBack.setAxis(Rotate.Y_AXIS);
        showBack.setFromAngle(90);
        showBack.setToAngle(0);

        hideFront.setOnFinished(_ -> {
            front.setVisible(false);
            back.setVisible(true);
        });

        // Создаем последовательность анимаций
        SequentialTransition flip = new SequentialTransition(hideFront, showBack);
        flip.setOnFinished(_ -> {
            open = false;
            // На всякий случай сбрасываем стиль еще раз после анимации
            this.setStyle(BASE_STYLE);
        });
        flip.play();
    }

    /**
     * Метод для проверки, можно ли кликнуть на карту
     * @return true, если карта закрыта
     */
    public boolean isClickable() { return !open; }

    /**
     * Метод для получения линейного индекса карты в сетке
     * @param gridCols - количество столбцов в сетке
     * @return индекс карты в сетке
     */
    public int getLinearIndex(int gridCols) {
        return this.row * gridCols + this.col;
    }

    /**
     * Метод для установки изображения на карту
     * @param image изображение для установки
     */
    public void setFrontImage(Image image) {
        if (image != null) {
            this.front.setImage(image);
        }
    }
}
