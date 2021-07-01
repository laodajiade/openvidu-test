package io.openvidu.server.domain;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.RandomStringUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class BasicDTO {
    private static final AtomicInteger inc = new AtomicInteger(0);

    private static final String prefix = RandomStringUtils.randomAlphabetic(3);

    @Setter
    @Getter
    private String trackId;

    public BasicDTO() {
        //trackId = prefix + String.format("%06d", inc.incrementAndGet());
    }
}
