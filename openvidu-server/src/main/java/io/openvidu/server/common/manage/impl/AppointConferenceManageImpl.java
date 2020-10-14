package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.RuidHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class AppointConferenceManageImpl implements AppointConferenceManage {

    @Resource
    AppointConferenceMapper appointConferenceMapper;
//
//    @Autowired
//    private ConfRoomManage confRoomManage;

    @Resource
    private UserMapper userMapper;


    //    public Optional<AppointConference> getByRuid(String ruid) {
//        AppointConferenceExample example = new AppointConferenceExample();
//        example.createCriteria().andRuidEqualTo(ruid);
//        return Optional.ofNullable(appointConferenceMapper.selectOneByExample(example));
//    }
//
//    public Optional<AppointConference> getByRoomIdInValidTime(String roomId) {
//        AppointConferenceExample example = new AppointConferenceExample();
//
//        example.createCriteria().andRoomIdEqualTo(roomId).andEndTimeGreaterThan(LocalDateTime.now());
//
//        return Optional.ofNullable(appointConferenceMapper.selectOneByExample(example));
//    }
//
//    public void deleteByRuid(String ruid) {
//        AppointConferenceExample example = new AppointConferenceExample();
//        example.createCriteria().andRuidEqualTo(ruid);
//        appointConferenceMapper.deleteByExample(example);
//    }
//
    public boolean isConflict(AppointmentRoomVO vo) {

        // 判断会议是否冲突
        AppointConference ac = new AppointConference();
        ac.setRoomId(vo.getRoomId());
        ac.setStartTime(new Date(vo.getStartTime()));
        ac.setEndTime(DateUtil.getEndDate(ac.getStartTime(), vo.getDuration(), Calendar.MINUTE));
        ac.setRuid(vo.getRuid());


        List<AppointConference> list = appointConferenceMapper.getConflictAppointConferenceList(ac);
        return Objects.nonNull(list) && !list.isEmpty();
    }

    public Conference constructConf(RpcConnection rpcConnection, Request<AppointmentRoomVO> request) {
        Conference conference = new Conference();
        AppointmentRoomVO params = request.getParams();

        String roomId = StringUtils.isNotBlank(params.getRoomId()) ? params.getRoomId() : rpcConnection.getUserUuid();
        String moderatorUuid = rpcConnection.getUserUuid();


        // save conference info
        conference.setRoomId(roomId);

        //conference.setRuid(StringUtils.isNotBlank(params.getRuid()) ? params.getRuid() : RuidHelper.generateAppointmentId());
        conference.setConferenceSubject(params.getSubject());
        conference.setConferenceMode(params.getConferenceMode().getMode());
        conference.setUserId(rpcConnection.getUserId());
        conference.setPassword(params.getPassword());
        conference.setStatus(ConferenceStatus.NOT_YET.getStatus());
        conference.setStartTime(new Date(params.getStartTime()));

        conference.setRoomCapacity(0);//roomCapacity不再需要
        conference.setProject(rpcConnection.getProject());
        conference.setConferenceDesc(params.getDesc());
        conference.setModeratorUuid(moderatorUuid);
        return conference;
    }

    @Override
    public void insert(AppointmentRoomVO params, RpcConnection rpcConnection) {
        AppointConference ac = new AppointConference();

        ac.setRuid(RuidHelper.generateAppointmentId());
        ac.setRoomId(params.getRoomId());
        ac.setConferenceSubject(params.getSubject());
        ac.setConferenceDesc(params.getDesc());
        ac.setUserId(rpcConnection.getUserId());
        ac.setModeratorUuid(rpcConnection.getUserUuid());
        ac.setStartTime(new Date(params.getStartTime()));
        ac.setEndTime(new Date(params.getEndTime()));
        ac.setDuration(params.getDuration());
        ac.setRoomCapacity(params.getRoomCapacity());

        ac.setAutoInvite(params.getAutoCall() ? 1 : 0);
        ac.setProject(rpcConnection.getProject());
        ac.setType("N");
        appointConferenceMapper.insertSelective(ac);

        params.setRuid(ac.getRuid());
    }
}
