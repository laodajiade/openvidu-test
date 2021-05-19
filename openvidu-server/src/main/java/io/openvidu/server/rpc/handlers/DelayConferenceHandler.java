package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointParticipant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.handlers.appoint.CancelAppointmentRoomHandler;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Service(ProtocolElements.DELAY_CONFERENCE_METHOD)
@Slf4j
public class DelayConferenceHandler extends ExRpcAbstractHandler<JsonObject> {

    private static final long FIVE_MINUTE_MILLION_SECOND = 5L * 60 * 1000;

    @Autowired
    private CancelAppointmentRoomHandler cancelAppointmentRoomHandler;

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        BindValidate.notEmpty(params, "roomId");
        BindValidate.notEmpty(params, "delayMinute");

        String roomId = params.get("roomId").getAsString();
        int delayMinute = params.get("delayMinute").getAsInt();

        Session session = sessionManager.getSession(roomId);
        if (session == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        String ruid = session.getRuid();

        Date minStartDate = DateUtils.addMinutes(new Date(), delayMinute);
        AppointConference conflictConference = appointConferenceManage.getConflict(minStartDate, roomId);
        try {
            if (conflictConference != null) {
                String method;
                List<AppointParticipant> appointParticipants;
                System.out.println(conflictConference.getEndTime().getTime() - minStartDate.getTime());
                if (conflictConference.getEndTime().getTime() - minStartDate.getTime() <= FIVE_MINUTE_MILLION_SECOND) {
                    method = ProtocolElements.APPOINTMENT_CONFERENCE_CANCEL_NOTIFY_METHOD;
                    appointParticipants = appointParticipantMapper.selectByRuids(singletonList(ruid));
                    cancelAppointmentRoomHandler.cancelApponitment(conflictConference.getRuid());
                    String reason = "cancelByDelay";
                    Set<String> uuids = appointParticipants.stream().map(AppointParticipant::getUuid).collect(Collectors.toSet());
                    JsonObject notifyParams = new JsonObject();
                    notifyParams.addProperty("ruid", conflictConference.getRuid());
                    notifyParams.addProperty("roomId", conflictConference.getRoomId());
                    notifyParams.addProperty("roomSubject", conflictConference.getConferenceSubject());
                    notifyParams.addProperty("reason", reason);
                    notificationService.sendBatchNotificationUuidConcurrent(uuids, method, notifyParams);
                }
            }
        } catch (Exception e) {
            log.error("update conflict error ", e);
        }

        return RespResult.ok();
    }
}
