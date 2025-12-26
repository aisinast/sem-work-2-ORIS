package util;

import java.util.List;
import java.util.Map;

/**
 * Утилитарный класс для работы с JSON форматом.
 */
public class JsonUtil {

    /**
     * Преобразует Map в JSON строку.
     * Поддерживает вложенные структуры: Map, List, String, Number, Boolean.
     * @param map   структура для преобразования, где ключ - строка, значение - объект поддерживаемого типа
     * @return  JSON строка в формате объекта
     */
    public static String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        // Флаг для отслеживания первого элемента (чтобы не ставить лишнюю запятую)
        boolean first = true;

        // Проходим по всем записям в Map
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            // Если это не первый элемент, добавляем запятую
            if (!first) {
                json.append(",");
            }

            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            // Проверяем тип значения и добавляем его соответствующим образом
            if (value instanceof String) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else if (value instanceof Map) {
                json.append(mapToJson((Map<String, Object>) value));
            } else if (value instanceof List) {
                json.append(listToJson((List<?>) value));
            } else {
                json.append(value);
            }

            // Устанавливаем флаг, что первый элемент уже обработан
            first = false;
        }

        json.append("}");

        return json.toString();
    }

    /**
     * Преобразует List в JSON строку
     * @param list  список для преобразования
     * @return  JSON строка в формате массива
     */
    private static String listToJson(List<?> list) {
        StringBuilder json = new StringBuilder("[");
        // Флаг для отслеживания первого элемента
        boolean first = true;

        // Проходим по всем элементам списка
        for (Object item : list) {
            // Если это не первый элемент, добавляем запятую
            if (!first) {
                json.append(",");
            }

            // Проверяем тип элемента и добавляем его соответствующим образом
            if (item instanceof String) {
                json.append("\"").append(escapeJson(item.toString())).append("\"");
            } else {
                json.append(item);
            }

            // Устанавливаем флаг, что первый элемент уже обработан
            first = false;
        }

        json.append("]");

        return json.toString();
    }

    /**
     * Экранирует специальные символы в JSON строках.
     * @param str   исходная строка
     * @return  строка с экранированными спецсимволами
     */
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")   // Обратный слеш
                .replace("\"", "\\\"")    // Двойные кавычки
                .replace("\n", "\\n")     // Перевод строки
                .replace("\r", "\\r")     // Возврат каретки
                .replace("\t", "\\t");    // Табуляция
    }
}
