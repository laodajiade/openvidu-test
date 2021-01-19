package io.openvidu.server.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ValidPeriodHelper {

    public static long getBetween(LocalDateTime expireDate) {
        long second = ChronoUnit.SECONDS.between(LocalDateTime.now(), expireDate);
        // 不足一天的算1天
        if (second < 0 && second > -86399) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), expireDate);
    }
}
