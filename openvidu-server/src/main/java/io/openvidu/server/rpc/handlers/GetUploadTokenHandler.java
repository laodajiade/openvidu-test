package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.StringUtil;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/7/22 10:10
 */
@Service
public class GetUploadTokenHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String type = getStringParam(request, ProtocolElements.GET_UPLOAD_TOKEN_TYPE_PARAM);
        if (!Objects.equals("DeviceLog", type)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        // save upload token
        String uploadToken;
        cacheManage.setLogUploadToken(rpcConnection.getUserUuid(), uploadToken = StringUtil.getNonce(32));

        // resp
        JsonObject respObj = new JsonObject();
        respObj.addProperty("uploadToken", uploadToken);
        respObj.addProperty("uploadUrl", openviduConfig.getDevUploadUrl());
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }

}

