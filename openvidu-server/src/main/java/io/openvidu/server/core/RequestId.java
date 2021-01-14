package io.openvidu.server.core;

import org.apache.commons.lang3.RandomStringUtils;

public class RequestId {
    private static final ThreadLocal<String> ids = new ThreadLocal<>();

    public static String initId() {
        String id = RandomStringUtils.randomAlphabetic(6);
        ids.set(id);
        return id;
    }

    public static String getId() {
        return ids.get();
    }
}
