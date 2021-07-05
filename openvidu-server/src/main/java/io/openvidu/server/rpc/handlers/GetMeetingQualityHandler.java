package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service(ProtocolElements.GET_MEETING_QUALITY_METHOD)
public class GetMeetingQualityHandler extends ExRpcAbstractHandler<JsonObject> {
    @Override
    public RespResult<JsonObject> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        BindValidate.notEmpty(params, "uuid");
        BindValidate.notEmpty(params, "roomId");

        String uuid = getStringParam(request, "uuid");
        String roomId = getStringParam(request, "roomId");

        Session session = sessionManager.getSession(roomId);
        if (session == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }

        Optional<Participant> participantOp = session.getParticipantByUUID(uuid);

        if (!participantOp.isPresent()) {
            return RespResult.fail(ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
        }
        Participant participant = participantOp.get();
        notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.UPLOAD_MEETING_QUALITY_NOTIFY_METHOD, new JsonObject());

        // 最多等待端上2秒
        for (int i = 0; i < 66; i++) {
            JsonObject meetingQuality = cacheManage.getMeetingQuality(uuid);
            if (meetingQuality != null) {
                return RespResult.ok(meetingQuality);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(30);
            } catch (InterruptedException e) {
                //ignore
            }
        }

        return RespResult.fail(ErrorCodeEnum.UPLOAD_MEETING_QUALITY_TIMEOUT);
    }
}
