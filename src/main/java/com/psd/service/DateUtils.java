package com.psd.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static String formatDateTime(String dateTimeStr) {
        try {
            // Парсим строку в LocalDateTime
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // Форматируем в нужный формат
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
            return dateTime.format(formatter);

        } catch (Exception e) {
            System.err.println("Ошибка форматирования даты: " + e.getMessage());
            return dateTimeStr; // Возвращаем исходную строку в случае ошибки
        }
    }
    public static String format(LocalDateTime localDateTime){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
        return formatter.format(localDateTime);
    }
}
