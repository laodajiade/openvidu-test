package io.openvidu.server.rpc.handlers;

import com.alibaba.druid.util.StringUtils;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class ModifyPasswordHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String originalPassword = getStringParam(request, ProtocolElements.ORIGINAL_PASSWORD_PARAM);
        String newPassword = getStringParam(request, ProtocolElements.NEW_PASSWORD_PARAM);

        if (Objects.isNull(originalPassword) || Objects.isNull(newPassword)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                    request.getId(), null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        Long userId = rpcConnection.getUserId();
        User user = userManage.getUserByUserId(userId);
        if (Objects.isNull(user)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                    request.getId(), null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }

        if (!StringUtils.equals(originalPassword, user.getPassword())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                    request.getId(), null, ErrorCodeEnum.ORIGINAL_PASSWORD_ERROR);
            return;
        }

        user.setPassword(newPassword);
        if (userManage.modifyPassword(user) == 1) {
            // del the user token
            cacheManage.delUserToken(user.getUuid());
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
