package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 调试接口，通过这个接口，强制将会议转未MCU。
 */
@Slf4j
@Service(ProtocolElements.SET_CONFERENCE_MODE_METHOD)
public class SetConferenceModeHandler extends ExRpcAbstractHandler<JsonObject> {

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {

        String mode = BindValidate.notEmptyAndGet(params, "mode");
        String pwd = BindValidate.notEmptyAndGet(params, "pwd");
        String roomId = BindValidate.notEmptyAndGet(params, "roomId");

        if (!Objects.equals(pwd, "sudi123")) {
            return RespResult.ok();
        }

        Session session = sessionManager.getSession(roomId);
        if (session == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        log.info("调试接口，将会议转为MCU");
        session.getCompositeService().createComposite();
        session.getCompositeService().asyncUpdateComposite();
        return RespResult.ok();
    }
}
