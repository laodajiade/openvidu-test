package io.openvidu.server.utils;

import java.util.concurrent.TimeUnit;

public class SafeSleep {

    public static void sleep(long timeout, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(timeout);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    public static void sleepMilliSeconds(long timeout) {
        sleep(timeout, TimeUnit.MILLISECONDS);
    }

    public static void sleepSeconds(long timeout) {
        sleep(timeout, TimeUnit.SECONDS);
    }

    public static void sleepMinutes(long timeout) {
        sleep(timeout, TimeUnit.MINUTES);
    }


}
