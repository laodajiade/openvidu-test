package io.openvidu.server.living;

import io.openvidu.java.client.LivingProperties;
import io.openvidu.java.client.Recording;
import io.openvidu.server.core.Session;
import lombok.Data;

@Data
public class Living {

    private String id;
    private String sessionId;
    private long createdAt; // milliseconds (UNIX Epoch time)
    private String url;
    private boolean hasAudio = true;
    private boolean hasVideo = true;
    private String creatorUuid;
    private Recording.OutputMode outputMode;
    private LivingProperties livingProperties;

    public Living(Session session, String url, String creatorUuid, LivingProperties livingProperties) {
        this.id = session.getSessionId() + "_" + System.currentTimeMillis();
        this.sessionId = session.getSessionId();
        this.createdAt = System.currentTimeMillis();
        this.url = url;
        this.creatorUuid = creatorUuid;
        this.livingProperties = livingProperties;
        this.outputMode = livingProperties.outputMode();
    }

    public boolean hasAudio() {
        return hasAudio;
    }

    public boolean hasVideo() {
        return hasVideo;
    }
}
