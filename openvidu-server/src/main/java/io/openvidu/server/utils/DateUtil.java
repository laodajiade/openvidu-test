package io.openvidu.server.utils;

import io.openvidu.server.common.enums.SubtitleLanguageEnum;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    private static final String[] WEEKDAYS = { "周一", "周二", "周三", "周四", "周五", "周六", "周日" };

    public static final String FORMAT_YEAR_MONTH_DAY = "yyyyMMdd";
    public static final String DEFAULT_YEAR_MONTH_DAY = "yyyy-MM-dd";

    /**
     * 获取结束时间
     *
     * @param startTime 开始时间
     * @param duration  时间间隔
     * @param unit      单位
     * @return
     */
    public static Date getEndDate(Date startTime, Integer duration, Integer unit) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(unit, duration);
        return calendar.getTime();
    }

    /**
     * 获取会议记录文件日期格式
     * @param date Date
     * @param language SubtitleLanguageEnum
     * @return
     */
    public static String getSubtitleRecordTxtTimeHeader(Date date, SubtitleLanguageEnum language) {
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return SubtitleLanguageEnum.cn.equals(language) ?
                (ldt.format(DateTimeFormatter.ofPattern("MM月dd日 ")) + WEEKDAYS[ldt.getDayOfWeek().getValue() - 1])
                : (ldt.format(DateTimeFormatter.ofPattern("MM-dd ")) + ldt.getDayOfWeek());
    }

    public static String getTimeOfDate(long timeMillis) {
        LocalDateTime ldt = LocalDateTime.ofInstant(new Date(timeMillis).toInstant(), ZoneId.systemDefault());
        return ldt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public static String getDateFormat(Date date, String format) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(format));
    }

    public static Date getDifferenceDate(int field, int dValue) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(field, dValue);
        return calendar.getTime();
    }

}
