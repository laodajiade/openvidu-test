package io.openvidu.server.common.broker;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.*;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.EvictParticipantStrategy;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.job.AppointConferenceJobHandler;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.rpc.handlers.UrgedPeopleToEndHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author chosongi
 * @date 2020/7/10 11:01
 */

@Slf4j
@Component
public class ToOpenviduNotifyHandler {

    private static final Gson gson = new GsonBuilder().create();

    @Resource
    private RpcNotificationService rpcNotificationService;

    @Autowired
    private UrgedPeopleToEndHandler urgedPeopleToEndHandler;

    @Resource
    private CacheManage cacheManage;

    @Autowired
    AppointConferenceJobHandler appointConferenceJobHandler;

    @Autowired
    FixedRoomExpiredHandler fixedRoomExpiredHandler;

    @Resource
    private SessionManager sessionManager;

    private

    ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ToOpenviduNotifyHandler-thread-%d").setDaemon(true).build());

    void notify(String message) {
        log.info("channel:to:openvidu recv {}", message);
        JsonObject obj = gson.fromJson(message, JsonObject.class);
        String method = obj.get("method").getAsString();
        JsonObject params = obj.getAsJsonObject("params");

        executorService.execute(() -> {
            try {
                switch (method) {
                    case ToOpenviduElement.URGED_PEOPLE_TO_END_METHOD:
                        urgedPeopleToEndHandler.notifyToModerator(params);
                        break;
                    case "fixedRoomExpired":
                        fixedRoomExpiredHandler.processor(params);
                        break;
                    case ToOpenviduElement.EVICT_PARTICIPANT_BY_UUID_METHOD:
                        EndReason endReason = EndReason.valueOf(params.get("endReason").getAsString());
                        JsonArray evictStrategies = params.getAsJsonArray("evictStrategies");
                        List<EvictParticipantStrategy> es = new ArrayList<>();
                        for (JsonElement evictStrategy : evictStrategies) {
                            es.add(EvictParticipantStrategy.valueOf(evictStrategy.getAsString()));
                        }
                        sessionManager.evictParticipantByUUID(params.get("roomId").getAsString(), params.get("uuid").getAsString(),
                                es, endReason);
                        break;
                    case ToOpenviduElement.SEBD_INVITE_NOTICE:
                        appointConferenceJobHandler.sendInviteNoticy(
                                JSONObject.parseObject(params.get("appointConference").getAsString(), AppointConference.class),
                                params.get("ruid").getAsString()
                        );
                        break;
                    case ToOpenviduElement.UPDATE_DEVICE_INFO:
                        System.out.println("进入监听" + ToOpenviduElement.UPDATE_DEVICE_INFO);
                        sessionManager.updateDeviceInfo(params.get("id").getAsString(),params.get("devNum").getAsString(),params.get("devName").getAsString());
                        break;
                }
            } catch (Exception e) {
                log.error("ToOpenviduNotifyHandler error", e);
            }
        });

    }


}
