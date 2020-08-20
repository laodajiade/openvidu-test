package io.openvidu.server.common.broker;

import io.openvidu.server.common.enums.EvictParticipantStrategy;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chosongi
 * @date 2020/8/19 15:20
 */
@Slf4j
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private RpcNotificationService notificationService;

    private static final Pattern NEEDED_EXPIRE_KEY_PATTERN = Pattern.compile("^ws:link:exception:(\\d+):(\\w+)$");

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        Matcher matcher;
        String expiredKey = message.toString();
        if ((matcher = NEEDED_EXPIRE_KEY_PATTERN.matcher(expiredKey)).matches()) {
            log.info("receive expire ws link key ===> " + expiredKey);
            String privateId = matcher.group(2);
            RpcConnection rpcConnection = notificationService.getRpcConnection(privateId);
            if (Objects.nonNull(rpcConnection)) {
                sessionManager.evictParticipantWhenDisconnect(rpcConnection,
                        Arrays.asList(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR, EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION));
            } else {
                log.info("RpcConnection:{} already been closed.", privateId);
            }
        } else {
            log.info("discard invalid key:{}", expiredKey);
        }
    }

}
