package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service(ProtocolElements.APPLY_SHARE_METHOD)
public class ApplyShareHandler extends ExRpcAbstractHandler<JsonObject> {

    public static final Object share_lock = new Object();

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        String targetId = BindValidate.notEmptyAndGet(params, "targetId");

        Session session = sessionManager.getSession(rpcConnection.getSessionId());
        if (session == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        Optional<Participant> participantOptional = session.getParticipantByUUID(targetId);
        if (!participantOptional.isPresent()) {
            return RespResult.fail(ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
        }

        Optional<Participant> originatorOp = session.getParticipantByUUID(rpcConnection.getUserUuid());
        if (!originatorOp.isPresent()) {
            return RespResult.fail(ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
        }

        if (!checkPermission(originatorOp.get(), participantOptional.get())) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        synchronized (session.getSharingOrSpeakerLock()) {
            JsonObject result = new JsonObject();
            result.addProperty("roomId", session.getSessionId());

            result.addProperty("originator", originatorOp.get().getUuid());
            Optional<Participant> sharingPart = session.getSharingPart();
            if (sharingPart.isPresent()) {
                result.addProperty("shareId", sharingPart.get().getUuid());
                if (!sharingPart.get().getUuid().equals(targetId)) {
                    // 已有分享者
                    return RespResult.fail(ErrorCodeEnum.SHARING_ALREADY_EXIST, result);
                } else {
                    // 分享者是自己，直接成功
                    return RespResult.ok(result);
                }
            }
            sharingPart = participantOptional;
            result.addProperty("shareId", sharingPart.get().getUuid());
            sessionManager.setSharing(session, sharingPart.get(), originatorOp.get().getUuid());
            return RespResult.ok(result);
        }
    }

    public boolean checkPermission(Participant originator, Participant target) {
        if (!target.getRole().needToPublish()) {
            log.info("{} 不是发布者,不能分享", target.getUuid());
            return false;
        }
        if (originator.getRole() == OpenViduRole.MODERATOR) {
            return true;
        }
        return originator.getRole() == OpenViduRole.PUBLISHER && originator.getUuid().equals(target.getUuid());
    }
}
