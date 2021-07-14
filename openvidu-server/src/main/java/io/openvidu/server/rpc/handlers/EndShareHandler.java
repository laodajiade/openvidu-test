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
@Service(ProtocolElements.END_SHARE_METHOD)
public class EndShareHandler extends ExRpcAbstractHandler<JsonObject> {


    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        String targetId = BindValidate.notEmptyAndGet(params, "targetId");

        Session session = sessionManager.getSession(rpcConnection.getSessionId());
        if (session == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        synchronized (session.getSharingOrSpeakerLock()) {
            Optional<Participant> sharingPart = session.getSharingPart();
            if (!sharingPart.isPresent()) {
                return RespResult.ok();
            }

            Optional<Participant> targetOp = session.getParticipantByUUID(targetId);
            if (!targetOp.isPresent()) {
                return RespResult.fail(ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            }

            Optional<Participant> originatorOp = session.getParticipantByUUID(rpcConnection.getUserUuid());
            if (!originatorOp.isPresent()) {
                return RespResult.fail(ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            }

            if (!checkPermission(originatorOp.get(), targetOp.get(), sharingPart.get())) {
                return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
            }

            sessionManager.endSharing(session, sharingPart.get(), originatorOp.get().getUuid());
        }
        return RespResult.ok();
    }

    public boolean checkPermission(Participant originator, Participant target, Participant sharingPart) {
        if (originator.getRole() == OpenViduRole.MODERATOR) {
            return true;
        }
        return originator.getUuid().equals(sharingPart.getUuid());
    }
}
