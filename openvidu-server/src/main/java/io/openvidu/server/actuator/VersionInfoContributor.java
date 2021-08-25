package io.openvidu.server.actuator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class VersionInfoContributor implements InfoContributor {
    Map<String, String> versions = null;

    @Override
    public void contribute(Info.Builder builder) {

        if (versions == null) {
            ClassPathResource resource = new ClassPathResource("git.properties");
            try {
                List<String> lines = IOUtils.readLines(resource.getInputStream(), "UTF-8");
                versions = new HashMap<>();
                for (String line : lines) {
                    if (line.startsWith("git")) {
                        String[] split = line.split("=");
                        versions.put(split[0], split[1]);
                    }
                }
            } catch (IOException e) {
                log.error("read git.properties error", e);
                versions = Collections.emptyMap();
            }
        }

        builder.withDetail("version", versions);
    }
}
