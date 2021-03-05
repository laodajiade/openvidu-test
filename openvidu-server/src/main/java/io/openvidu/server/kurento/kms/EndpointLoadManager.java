package io.openvidu.server.kurento.kms;

import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.core.DeliveryKmsManager;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.endpoint.MediaChannel;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.MediaPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiFunction;

@Component
@Slf4j
public class EndpointLoadManager implements LoadManager {


    private static SessionManager sessionManager = null;

    private static BiFunction<String, Integer, Integer> incFunction = (k, v) -> v == null ? 1 : ++v;
    private static BiFunction<String, Integer, Integer> inc2 = (k, v) -> v == null ? 2 : (v = v + 2);

    private static final String UNKNOWN = "unknown";

    @Autowired
    public void setSessionManager(SessionManager sessionManager) {
        EndpointLoadManager.sessionManager = sessionManager;
    }

    @Override
    public int calculateLoad(Kms kms) {
        return kms.getKurentoClient().getServerManager().getPipelines().size();
    }

    public static Kms getLessKms(Collection<Kms> kmss) {
        if (kmss.size() == 1) {
            return kmss.iterator().next();
        }
        Map<String, String> pipelineAndKmsMap = new HashMap<>();
        Map<String, Integer> publisherCntMap = new HashMap<>();
        Map<String, Integer> subscriberCntMap = new HashMap<>();


        for (Kms kms : kmss) {
            List<MediaPipeline> pipelines = kms.getKurentoClient().getServerManager().getPipelines();
            for (MediaPipeline pipeline : pipelines) {
                pipelineAndKmsMap.put(pipeline.getId(), kms.getIp());
            }

        }

        Collection<Session> sessions = sessionManager.getSessions();

        for (Session session : sessions) {
            KurentoSession kurentoSession = (KurentoSession) session;
            Set<Participant> participants = kurentoSession.getParticipants();
            for (Participant participant : participants) {
                KurentoParticipant kp = (KurentoParticipant) participant;
                if (kp.isPublisherStreaming()) {
                    publisherCntMap.compute(pipelineAndKmsMap.getOrDefault(kp.getPipeline().getId(), UNKNOWN), incFunction);
                }

                for (SubscriberEndpoint value : kp.getSubscribers().values()) {
                    if (value.getStreamId() != null) {
                        subscriberCntMap.compute(pipelineAndKmsMap.getOrDefault(value.getPipeline().getId(), UNKNOWN), incFunction);
                    }
                }
            }

            // 计算分发的压力
            for (DeliveryKmsManager deliveryKmsManager : kurentoSession.getDeliveryKmsManagers()) {
                for (MediaChannel mediaChannel : deliveryKmsManager.dispatcherMap.values()) {
                    String deliveryIp = pipelineAndKmsMap.getOrDefault(mediaChannel.getTargetPipeline().getId(), UNKNOWN);
                    publisherCntMap.compute(deliveryIp, incFunction);

                    String masterIp = pipelineAndKmsMap.getOrDefault(mediaChannel.getSourcePipeline().getId(), UNKNOWN);
                    subscriberCntMap.compute(masterIp, incFunction);
                }
            }
        }


        log.info("publisherCntMap {}", publisherCntMap);
        log.info("subscriberCntMap {}", subscriberCntMap);

        Map<String, Integer> scoreMap = new HashMap<>();
        publisherCntMap.forEach((k, v) -> {
            scoreMap.put(k, 2 * v);
        });

        subscriberCntMap.forEach((k, v) -> {
            scoreMap.compute(k, (k1, v1) -> v1 == null ? v : (v1 + v));
        });
        log.info("scoreMap {}", scoreMap);
        scoreMap.remove("unknown");

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scoreMap.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getValue));

        String minIp = list.get(0).getKey();
        for (Kms kms : kmss) {
            if (Objects.equals(minIp, kms.getIp())) {
                return kms;
            }
        }

        return new ArrayList<>(kmss).get(0);
    }

    public static DeliveryKmsManager getLessDeliveryKms(Collection<DeliveryKmsManager> dkmss) {
        if (dkmss == null || dkmss.isEmpty()) {
            throw new IllegalArgumentException("deliveryKmsManager list are null");
        }
        if (dkmss.size() == 1) {
            return dkmss.iterator().next();
        }

        Map<String, DeliveryKmsManager> deliveryKmsMap = new HashMap<>();
        List<Kms> kmss = new ArrayList<>();
        for (DeliveryKmsManager deliveryKmsManager : dkmss) {
            deliveryKmsMap.put(deliveryKmsManager.getKms().getIp(), deliveryKmsManager);
            kmss.add(deliveryKmsManager.getKms());
        }

        Kms lessKms = getLessKms(kmss);
        return deliveryKmsMap.getOrDefault(lessKms.getIp(), dkmss.iterator().next());
    }

}
