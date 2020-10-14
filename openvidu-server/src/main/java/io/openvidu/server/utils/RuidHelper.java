package io.openvidu.server.utils;

import java.util.UUID;

public class RuidHelper {


    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    private static String generateId(String prefix) {
        return prefix + "-" + generateId();
    }


    public static String generateAppointmentId() {
        return generateId("appt");
    }

    public static String generateGenericId() {
        return generateId("gene");
    }

}
