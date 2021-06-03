package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;

import java.util.Date;
import java.util.Optional;

public interface AppointConferenceManage {

    boolean isConflict(AppointmentRoomVO vo);

    boolean isConflict(String ruid, String roomId, Date startTime, Date endTime);

    void insert(AppointmentRoomVO params, RpcConnection rpcConnection);

    AppointConference getByRuid(String ruid);

    void updateAppointment(AppointConference appt, AppointmentRoomVO vo);

    void deleteByRuid(String ruid);

    void updateById(AppointConference appt);

    Optional<AppointConference> getConflict(Date startTime, String roomId);

    Optional<AppointConference> getNextAppt(Date startTime, String roomId);

}
