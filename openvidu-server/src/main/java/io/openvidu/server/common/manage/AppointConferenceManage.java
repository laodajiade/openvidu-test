package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;

import java.util.Date;

public interface AppointConferenceManage {

    boolean isConflict(AppointmentRoomVO vo);

    void insert(AppointmentRoomVO params, RpcConnection rpcConnection);

    AppointConference getByRuid(String ruid);

    void updateAppointment(AppointConference appt, AppointmentRoomVO vo);

    void deleteByRuid(String ruid);

    void updateById(AppointConference appt);

    AppointConference getConflict(Date startTime, String roomIde);
}
