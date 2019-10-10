package io.openvidu.server.common.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.Contants.CacheKeyConstants;
import io.openvidu.server.common.broker.RedisPublisher;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.core.KurentoParticipant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2019/9/12 14:50
 */
@Slf4j
@Component
public class CacheManageImpl implements CacheManage {

    private static final Gson gson = new GsonBuilder().create();

    @Resource(name = "tokenStringTemplate")
    private StringRedisTemplate tokenStringTemplate;

    @Resource
    RedisPublisher publisher;


    @Override
    public boolean accessTokenEverValid(String userId, String token) {
        boolean result;
        try {
            result = token.equals(tokenStringTemplate.opsForHash().entries(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userId).get("token").toString());
        } catch (Exception e) {
            log.error("Exception:", e);
            return false;
        }
        return result;
    }

    /*@PostConstruct
    public void init() {
//        Map<Object, Object> map = tokenStringTemplate.opsForHash().entries("token:666");
//        System.out.println(map);
        publisher.sendChannelMsg("chn_client_request", "test_request");
    }*/

    @Override
    public void removeSession(String sessionId) {
        tokenStringTemplate.opsForSet().remove(CacheKeyConstants.CONFERENCES_KEY, sessionId);
    }

    @Override
    public void recordSession(String sessionId) {
        tokenStringTemplate.opsForSet().add(CacheKeyConstants.CONFERENCES_KEY, sessionId);
    }

    @Override
    public void recordPartAndRelation(Participant participant) {
        String partkey = CacheKeyConstants.PARTICIPANT_PREFIX_KEY + participant.getParticipantPrivateId() +
                CacheKeyConstants.SEPARATOR + participant.getStreamType().name();
        Map<String, Object> partAttriMap = new HashMap<>();
        partAttriMap.put("userId", gson.fromJson(participant.getClientMetadata(), JsonObject.class).get("clientData").getAsString());
        partAttriMap.put("role", participant.getStreamType().name());
        partAttriMap.put("publicId", participant.getParticipantPublicId());
        partAttriMap.put("createdAt", String.valueOf(participant.getCreatedAt()));
        partAttriMap.put("metadata", participant.getClientMetadata());

        tokenStringTemplate.opsForHash().putAll(partkey, partAttriMap);
        // record relation between conference and participant
        tokenStringTemplate.opsForSet().add(CacheKeyConstants.PARTICIPANTS_OF_CONFERENCE_PREFIX_KEY
                + participant.getSessionId(), partkey);
    }

    @Override
    public void updateParticipantStreamInfo(KurentoParticipant kParticipant) {
        String partkey = CacheKeyConstants.PARTICIPANT_PREFIX_KEY + kParticipant.getParticipantPrivateId() +
                CacheKeyConstants.SEPARATOR + kParticipant.getStreamType().name();
        Map<String, Object> partAttriMap = new HashMap<>();
        partAttriMap.put("streamId", kParticipant.getPublisherStreamId());
        partAttriMap.put("streamType", kParticipant.getStreamType().name());
        partAttriMap.put("hasAudio", String.valueOf(kParticipant.getPublisherMediaOptions().hasAudio()));
        partAttriMap.put("hasVideo", String.valueOf(kParticipant.getPublisherMediaOptions().hasVideo()));
        partAttriMap.put("audioActive", String.valueOf(kParticipant.getPublisherMediaOptions().isAudioActive()));
        partAttriMap.put("videoActive", String.valueOf(kParticipant.getPublisherMediaOptions().isVideoActive()));
        partAttriMap.put("handStatus", kParticipant.getHandStatus().name());

        tokenStringTemplate.opsForHash().putAll(partkey, partAttriMap);
    }

    @Override
    public void removeParticipant(Participant participant) {
        String partkey = CacheKeyConstants.PARTICIPANT_PREFIX_KEY + participant.getSessionId()
                + CacheKeyConstants.SEPARATOR + participant.getStreamType().name();
        if (Objects.equals(tokenStringTemplate.delete(partkey), Boolean.TRUE))
            tokenStringTemplate.opsForSet().remove(CacheKeyConstants.PARTICIPANTS_OF_CONFERENCE_PREFIX_KEY
                    + participant.getSessionId(), partkey);
    }

}
