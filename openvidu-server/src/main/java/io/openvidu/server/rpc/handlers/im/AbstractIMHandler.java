package io.openvidu.server.rpc.handlers.im;

import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ProtocolElements.SEND_MSG_METHOD)
public abstract class AbstractIMHandler<P> extends ExRpcAbstractHandler<P> {


    protected String getRecentUsername(String uuid, String defaultName) {
        final User user = userMapper.selectByUUID(uuid);
        if (user == null) {
            return defaultName;
        }
        return user.getUsername();
    }
}
