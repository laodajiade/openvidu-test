package io.openvidu.server.rpc.handlers;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
public class StartPtzControlHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
          String connectionId = getStringParam(request, ProtocolElements.START_PTZ_CONTROL_CONNECTIONID_PARM);
          int operateCode = getIntParam(request, ProtocolElements.START_PTZ_CONTROL_OPERATE_CODE_PARM);
          Long maxDuration = getLongParam(request, ProtocolElements.START_PTZ_CONTROL_MAX_DURATION_PARM);
          // verify current user role
          if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(rpcConnection.getSessionId(),
                rpcConnection.getParticipantPrivateId()).getRole())) {
                     this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
                    return;
          }
          RpcConnection rpc = lookingDevice(rpcConnection,connectionId);
          String serialNumber = rpc.getSerialNumber();
          if (StringUtils.isEmpty(serialNumber)) {
               notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
               return;
          }
          JsonObject notifyResult = new JsonObject();
          notifyResult.addProperty(ProtocolElements.START_PTZ_CONTROL_SERIAL_NUMBER_PARM, serialNumber);
          notifyResult.addProperty(ProtocolElements.START_PTZ_CONTROL_OPERATE_CODE_PARM, operateCode);
          notifyResult.addProperty(ProtocolElements.START_PTZ_CONTROL_MAX_DURATION_PARM, maxDuration);
          notificationService.sendNotification(rpc.getParticipantPrivateId(), ProtocolElements.START_PTZ_CONTROL_METHOD, notifyResult);
          this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
