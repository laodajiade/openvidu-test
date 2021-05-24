package io.openvidu.server.common.broker;

import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointConferenceExample;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.handlers.appoint.CancelAppointmentRoomHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Component
public class FixedRoomExpiredHandler {

    @Resource
    private SessionManager sessionManager;

    @Autowired
    private AppointConferenceMapper appointConferenceMapper;

    @Autowired
    private CancelAppointmentRoomHandler cancelAppointmentRoomHandler;

    public void processor(JsonObject params) {
        String roomId = params.get("roomId").getAsString();

        Session session = sessionManager.getSession(roomId);
        if (session != null) {
            sessionManager.closeSession(roomId, EndReason.fixedRoomServiceExpired);
        }

        AppointConferenceExample example = new AppointConferenceExample();
        example.createCriteria().andRoomIdEqualTo(roomId).andStatusIn(Arrays.asList(0, 1));
        List<AppointConference> appointConferenceList = appointConferenceMapper.selectByExample(example);

        for (AppointConference appointConference : appointConferenceList) {
            cancelAppointmentRoomHandler.cancelApponitment(appointConference.getRuid());
        }
    }
}
