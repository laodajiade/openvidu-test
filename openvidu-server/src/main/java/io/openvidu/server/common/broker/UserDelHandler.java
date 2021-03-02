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

    /**

     I looked at him. 'There's a woman in this somewhere, isn't there? You've had a strange look in your eyes for weeks.'

     Will laughed, but didn't answer my question.

     The theatres in London didn't open again until June 1594. Will often visited Lord Southampton, but sometimes we went on tour with the company, or spent time at home in Stratford. Will began to spend more time in Stratford, because it was quiet there, and he could do his writing. I never heard what Anne thought about it all.

     During those years Will wrote a lot of poetry. He wrote his beautiful long poem, Venus and Adonis, for his friend Lord Southampton, and he wrote many of his famous short poems, the Sonnets. But they didn't go in a book; they were only for his friends to read.

     One day, when we were back in London, I was reading some of his latest sonnets. Will was out somewhere, and I was at home in our lodgings in Bishopsgate. A lot of the poems were about a woman, a terrible, black-haired, black-eyed woman. She was cold and cruel, then she was true and loving, and then she was cruel again.

     For I have sworn thee fair, and thought the bright,

     Who art as black as hell, as dark as night.

     Was Will writing about himself here? I asked myself. And who was this woman, this Dark Lady?

     I always like to know what's going on, so I listened, and watched, and looked at all his women friends.

     Then one day I saw her. I was coming in the door at our lodgings, and she was coming downstairs. She had black hair and great stormy black eyes, and there was gold at her ears and round her neck. I stood back and she went past me like a ship sailing into war. She looked wild, and angry, and very, very beautiful.

     'Whew!' I said to myself. 'If that's Will's Dark Lady, he'll never have a quiet, easy life!'

     The woman looked Italian, so I went and asked John Florio about her. Florio was Lord Southampton's Italian teacher. We saw a lot of him in those days.

     I described the woman, and he knew her at once.

     'Emilia,' he said. 'Emilia Bassano. Now Emilia Lanier, wife to Alphonso Lanier. Before that, she lived with the old Lord Chamberlain. She was not his wife, you understand. But why do you want to know, my friend?'

     'If she's a married lady, she doesn't have a lover now, then?'

     Florio laughed loudly. 'Lovers! You don't know Emilia Lanier! She's a bad woman, my friend, a bad woman.' Now he spoke very quietly. 'For a time she was the friend of Lord Southampton. But not now. That is all finished.'

     I didn't ask him about Will. Perhaps Emilia Lanier was Will's Dark Lady, or perhaps Will was just trying to help his friend Lord Southampton. Nobody will ever know now.

     */
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
                            && Objects.isNull(userManage.getUserByUserId(userId = delUserObj.get("userId").getAsLong()))) {
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
                                            Arrays.asList(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR, EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION));
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
