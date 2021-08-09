package io.openvidu.server.utils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public class LocalDateTimeUtils {

    private static LocalTime MAX = LocalTime.MAX.minusNanos(999_999_999);

    /**
     * 当月的第一天
     */
    public static LocalDateTime firstDayOfMonth() {
        return firstDayOfMonth(LocalDate.now());
    }

    /**
     * 当月的第一天
     */
    public static LocalDateTime firstDayOfMonth(LocalDate localDate) {
        LocalDate firstDayOfMonth = localDate.with(TemporalAdjusters.firstDayOfMonth());
        return LocalDateTime.of(firstDayOfMonth, LocalTime.MIN);
    }

    /**
     * 一天的0点
     */
    public static LocalDateTime minTimeOfDay() {
        return LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
    }

    /**
     * 一天的0点
     */
    public static LocalDateTime minTimeOfDay(LocalDateTime localDateTime) {
        return LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MIN);
    }

    /**
     * 一天的23:59:59 000点
     */
    public static LocalDateTime maxTimeOfDay(LocalDateTime localDateTime) {
        return maxTimeOfDay(localDateTime.toLocalDate());
    }

    /**
     * 一天的23:59:59 000点
     */
    public static LocalDateTime maxTimeOfDay(LocalDate localDate) {
        return LocalDateTime.of(localDate, MAX);
    }

    /**
     * 一周的周一
     */
    public static LocalDateTime mondayOfWeek() {
        return mondayOfWeek(LocalDate.now());
    }

    /**
     * 一周的周一
     */
    public static LocalDateTime mondayOfWeek(LocalDate localDate) {
        LocalDate monday = localDate.with(DayOfWeek.MONDAY);
        return LocalDateTime.of(monday, LocalTime.MIN);
    }

    /**
     * 转换为时间
     */
    public static Long toEpochSecond(LocalDateTime localDateTime) {
        return localDateTime.toEpochSecond(BeiJingZoneOffset.of());
    }

    /**
     * 转换为时间
     */
    public static Long toEpochMilli(LocalDateTime localDateTime) {
        return localDateTime.toInstant(BeiJingZoneOffset.of()).toEpochMilli();
    }

    /**
     * 转换为时间
     */
    public static LocalDateTime fromEpochSecond(Integer unixTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTime), BeiJingZoneOffset.of());
    }

    public static LocalDateTime translateFromDate(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        return instant.atZone(zoneId).toLocalDateTime();
    }

    public static Date translateToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.toInstant(BeiJingZoneOffset.of()));
    }

}
