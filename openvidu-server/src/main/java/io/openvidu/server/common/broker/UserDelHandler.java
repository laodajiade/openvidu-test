package io.openvidu.server.common.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.EvictParticipantStrategy;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author chosongi
 * @date 2020/7/1 11:13
 */
@Slf4j
@Component
public class UserDelHandler {

    private static final Gson gson = new GsonBuilder().create();

    private static BlockingQueue<String> delUserInfos = new LinkedBlockingDeque<>(100);

    @Resource
    private SessionManager sessionManager;

    @Resource
    private CacheManage cacheManage;

    @Resource
    private UserManage userManage;

    @Resource
    private RpcNotificationService rpcNotificationService;

    @PostConstruct
    public void init() {
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < 1; i++) {
            cachedThreadPool.execute(new UserDelThread());
        }
    }

    static void accessOutDeletedUser(String delInfo) {
        log.info("User delete info:{} is offering in the queue.", delInfo);
        delUserInfos.offer(delInfo);
    }

    private class UserDelThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                Long userId;
                String uuid = null;
                JsonObject delUserObj;
                try {
                    delUserObj = gson.fromJson(delUserInfos.take(), JsonObject.class);
                    if (delUserObj.has("userId")
                           // && Objects.isNull(userManage.getUserByUserId(userId = delUserObj.get("userId").getAsLong())
                    ) {
                        userId = delUserObj.get("userId").getAsLong();
                        // find the websocket connection with userId
                        RpcConnection delUserRpcConnection = rpcNotificationService.getRpcConnections()
                                .stream().filter(rpcConnection -> Objects.equals(userId, rpcConnection.getUserId())
                                        && AccessTypeEnum.terminal.equals(rpcConnection.getAccessType()))
                                .max(Comparator.comparing(RpcConnection::getCreateTime))
                                .orElse(null);

                        if (Objects.nonNull(delUserRpcConnection)) {
                            uuid = delUserRpcConnection.getUserUuid();
                            // check user deleted ever in conference
                            String sessionId;
                            Session session;
                            Participant participant;
                            if (!StringUtils.isEmpty(sessionId = delUserRpcConnection.getSessionId())
                                    && Objects.nonNull(session = sessionManager.getSession(sessionId))
                                    && Objects.nonNull(participant = session.getParticipantByUserId(userId))) {
                                log.info("Evict participant:{} from session: {} and access out the user:{} websocket.", participant.getUuid(), sessionId, userId);
                                if (OpenViduRole.MODERATOR.equals(participant.getRole())) {
                                    // close the session
                                    log.info("Close the session cause the deleted user is moderator:{}.", participant.getUuid());
                                    sessionManager.dealSessionClose(participant.getSessionId(), EndReason.closeSessionByModerator);
                                } else {
                                    // evict participant from conference and change the layout
                                    sessionManager.evictParticipantByUUID(session.getSessionId(), participant.getUuid(),
                                            Arrays.asList(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR, EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION),
                                            EndReason.forceDisconnectByServer);
                                }
                            } else {
                                // access out the delUserRpcConnection directly
                                log.info("Access out the user:{} websocket connection directly cause it is free", userId);
                            }
                            rpcNotificationService.sendNotification(delUserRpcConnection.getParticipantPrivateId(), ProtocolElements.EVICT_CORPORATION_METHOD,new JsonObject());
                            TimeUnit.SECONDS.sleep(3);

                            rpcNotificationService.closeRpcSession(delUserRpcConnection.getParticipantPrivateId());
                        } else {
                            log.info("User:{} deleted did not access the signal server.", userId);
                        }

                        // del user token info in cache
                        cacheManage.delUserToken(uuid);
                    } else {
                        log.error("Invalid user delete info:{}", delUserObj.toString());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
