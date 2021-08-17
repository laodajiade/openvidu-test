package io.openvidu.server.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class StartTimeInfoContributor implements InfoContributor {

    private final LocalDateTime startTime;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StartTimeInfoContributor() {
        startTime = LocalDateTime.now();
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("startTime", startTime.format(formatter));
    }
}
