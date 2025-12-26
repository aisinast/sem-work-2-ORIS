package model;

import java.util.regex.Pattern;

/**
 * Протокол обмена данными для игры.
 * Используется для обмена данными между клиентом и сервером.
 */
public class Protocol {
    /**
     * Системные сообщения.
     * Формат: "0|отправитель|текст_сообщения"
     */
    public static final int TYPE_SYSTEM = 0;
    /**
     * Подключение нового игрока.
     * Формат: "1|имя_игрока"
     */
    public static final int TYPE_CONNECT = 1;
    /**
     * Открытие карточки.
     * Формат: "2|позиция_карточки"
     */
    public static final int TYPE_CARD_OPEN = 2;
    /**
     * Сообщение в чат.
     * Формат: "3|отправитель|текст_сообщения"
     */
    public static final int TYPE_CHAT_MESSAGE = 3;
    /**
     * Завершение игры.
     * Формат: "4|победители|максимальный_счет"
     * Победители перечисляются через точку с запятой ("Игрок1;Игрок2")
     */
    public static final int TYPE_GAME_OVER = 4;
    /**
     * Обработка ошибок.
     * Формат: "5|текст_ошибки"
     */
    public static final int TYPE_ERROR = 5;
    /**
     * Обновление состояния игры.
     * Формат: "6|json_состояния"
     * JSON содержит полное состояние игры: поле, счета, текущего игрока, флаги.
     */
    public static final int TYPE_GAME_STATE = 6;
    /**
     * Начало игры.
     * Формат: "7"
     */
    public static final int TYPE_START_GAME = 7;
    /**
     * Смена хода.
     * Формат: "8|имя_игрока"
     */
    public static final int TYPE_PLAYER_TURN = 8;
    /**
     * Сброс игры.
     * Формат: "9"
     */
    public static final int TYPE_GAME_RESET = 9;

    // Разделитель типа сообщения и его параметров.
    public static final String SEPARATOR = "|";
    // Имя отправителя для системных сообщений
    public static final String SYSTEM_USER = "Система";

    /**
     * Кодирует сообщение в строку по протоколу.
     *
     * @param type  тип сообщения
     * @param parts чисти сообщения
     * @return  строка в формате протокола, готовая к отправке
     */
    public static String encode(int type, String... parts) {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        for (String part : parts) {
            sb.append(SEPARATOR).append(part);
        }
        return sb.toString();
    }

    /**
     * Декодирует строку протокола в массив частей сообщения.
     * Разделяет строку по разделителю SEPARATOR.
     *
     * @param message строка в формате протокола
     * @return массив строк, где нулевой элемент - тип сообщения, а остальные - параметры
     */
    public static String[] decode(String message) {
        return message.split(Pattern.quote(SEPARATOR));
    }
}