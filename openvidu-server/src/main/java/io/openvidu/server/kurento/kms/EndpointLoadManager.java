package io.openvidu.server.kurento.kms;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.core.DeliveryKmsManager;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.MediaPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BiFunction;

@Component
@Slf4j
public class EndpointLoadManager implements LoadManager {


    private static SessionManager sessionManager = null;

    private static final BiFunction<String, Integer, Integer> incFunction = (k, v) -> v == null ? 1 : ++v;

    private static final String UNKNOWN = "unknown";

    private static CacheManage cacheManage;

    @Autowired
    public void setSessionManager(SessionManager sessionManager) {
        EndpointLoadManager.sessionManager = sessionManager;
    }

    @Autowired
    public void setCacheManage(CacheManage cacheManage) {
        EndpointLoadManager.cacheManage = cacheManage;
    }

    @Override
    public int calculateLoad(Kms kms) {
        return kms.getKurentoClient().getServerManager().getPipelines().size();
    }

    public static Map<String, Integer> calculateLoad(Collection<Kms> kmss) {
        Map<String, Integer> loadMap = new HashMap<>();

        for (Kms kms : kmss) {
            List<MediaPipeline> pipelines = kms.getKurentoClient().getServerManager().getPipelines();
            int load = 0;
            for (MediaPipeline pipeline : pipelines) {
                log.info("loadMap {}", pipeline.getId());
                load += cacheManage.getPipelineLoad(pipeline.getId());
            }
            loadMap.put(kms.getId(), load);
        }
        log.info("loadMap {}", loadMap);
        return loadMap;
    }

    public static Kms getLessKms(Collection<Kms> kmss) {
        if (CollectionUtils.isEmpty(kmss)) {
            return null;
        }
        if (kmss.size() == 1) {
            return kmss.iterator().next();
        }

        Map<String, Integer> loadMap = calculateLoad(kmss);

        List<Map.Entry<String, Integer>> list = new ArrayList<>(loadMap.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getValue));

        String minId = list.get(0).getKey();
        for (Kms kms : kmss) {
            if (Objects.equals(minId, kms.getId())) {
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
            deliveryKmsMap.put(deliveryKmsManager.getKms().getId(), deliveryKmsManager);
            kmss.add(deliveryKmsManager.getKms());
        }

        Kms lessKms = getLessKms(kmss);
        return deliveryKmsMap.getOrDefault(lessKms.getId(), dkmss.iterator().next());
    }

}
