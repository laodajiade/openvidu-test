package io.openvidu.server.common.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.AccessTypeEnum;
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
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

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
                JsonObject delUserObj;
                try {
                    delUserObj = gson.fromJson(delUserInfos.take(), JsonObject.class);
                    if (delUserObj.has("userId")) {
                        Long userId = delUserObj.get("userId").getAsLong();
                        // find the websocket connection with userId
                        RpcConnection delUserRpcConnection = rpcNotificationService.getRpcConnections()
                                .stream().filter(rpcConnection -> Objects.equals(userId, rpcConnection.getUserId())
                                        && !StringUtils.isEmpty(rpcConnection.getSerialNumber())
                                        && AccessTypeEnum.terminal.equals(rpcConnection.getAccessType()))
                                .max(Comparator.comparing(RpcConnection::getCreateTime))
                                .orElse(null);
                        if (Objects.nonNull(delUserRpcConnection)) {
                            // check user deleted ever in conference
                            String sessionId;
                            Session session;
                            Participant participant;
                            if (!StringUtils.isEmpty(sessionId = delUserRpcConnection.getSessionId())
                                    && Objects.nonNull(session = sessionManager.getSession(sessionId))
                                    && Objects.nonNull(participant = session.getParticipantByUserId(userId.toString()))) {
                                // evict participant from conference
                                log.info("Evict participant:{} from session: {} and access out the user:{} websocket " +
                                        "connection directly cause it is free", participant.getUuid(), sessionId, userId);
                                sessionManager.evictParticipant(participant, null, null, EndReason.forceDisconnectByServer);
                            } else {
                                // access out the delUserRpcConnection directly
                                log.info("Access out the user:{} websocket connection directly cause it is free", userId);
                            }
                            rpcNotificationService.closeRpcSession(delUserRpcConnection.getParticipantPrivateId());
                        } else {
                            log.info("User:{} deleted did not access the signal server.", userId);
                        }
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
