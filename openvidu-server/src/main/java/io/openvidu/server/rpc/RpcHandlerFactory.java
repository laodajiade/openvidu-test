package io.openvidu.server.rpc;

import io.openvidu.server.rpc.handlers.AccessInHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.openvidu.client.internal.ProtocolElements.ACCESS_IN_METHOD;

/**
 * @author chosongi
 * @date 2019/11/5 14:27
 */
@Slf4j
@Component
public class RpcHandlerFactory {

    private static final Map<String, RpcAbstractHandler> handlersMap = new ConcurrentHashMap<>(100);

    @Resource
    private AccessInHandler accessInHandler;

    @PostConstruct
    public void init() {
        handlersMap.put(ACCESS_IN_METHOD, accessInHandler);
    }

    public RpcAbstractHandler getRpcHandler(String requestMethod) {
        return handlersMap.getOrDefault(requestMethod, null);
    }

}
