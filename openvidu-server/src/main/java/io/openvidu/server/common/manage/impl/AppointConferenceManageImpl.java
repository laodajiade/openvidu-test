package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.manage.RoomManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointConferenceExample;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.RuidHelper;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class AppointConferenceManageImpl implements AppointConferenceManage {

    @Resource
    AppointConferenceMapper appointConferenceMapper;

    @Resource
    ConferenceMapper conferenceMapper;

    @Resource
    RoomManage roomManage;

    public void deleteByRuid(String ruid) {
        AppointConferenceExample example = new AppointConferenceExample();
        example.createCriteria().andRuidEqualTo(ruid);
        appointConferenceMapper.deleteByExample(example);
    }

    @Override
    public void updateById(AppointConference appt) {
        appointConferenceMapper.updateByPrimaryKey(appt);
    }

    @Override
    public Optional<AppointConference> getConflict(Date startTime, String roomId) {
        AppointConferenceExample example = new AppointConferenceExample();
        example.createCriteria().andRoomIdEqualTo(roomId).andStartTimeLessThan(startTime).andStatusEqualTo(0);
        example.setOrderByClause("start_time asc");
        return appointConferenceMapper.selectOneByExample(example);
    }

    @Override
    public Optional<AppointConference> getNextAppt(Date startTime, String roomId) {
        AppointConferenceExample example = new AppointConferenceExample();
        example.createCriteria().andRoomIdEqualTo(roomId).andStartTimeGreaterThan(startTime).andStatusEqualTo(0);
        example.setOrderByClause("start_time asc");
        return appointConferenceMapper.selectOneByExample(example);
    }


    public boolean isConflict(AppointmentRoomVO vo) {
        // 判断会议是否冲突
        AppointConference ac = new AppointConference();
        ac.setRoomId(vo.getRoomId());
        ac.setStartTime(new Date(vo.getStartTime()));
        ac.setEndTime(DateUtil.getEndDate(ac.getStartTime(), vo.getDuration(), Calendar.MINUTE));
        ac.setRuid(vo.getRuid());

        List<AppointConference> list = appointConferenceMapper.getConflictAppointConferenceList(ac);
        boolean isConflict = Objects.nonNull(list) && !list.isEmpty();
        if (isConflict) {
            return isConflict;
        }

        // 如果开始时间再2分钟内，则判断现在有没有固定会议室在开会
        if ((vo.getStartTime() - System.currentTimeMillis() <= 120000) && RoomIdTypeEnums.isFixed(vo.getRoomId())) {
            Conference conference = conferenceMapper.selectUsedConference(vo.getRoomId());
            if (conference != null) {
                log.info("create appoint conflict because exist fixed used ruid {}", conference.getRuid());
            }
            return conference != null;
        }
        return false;
    }

    public boolean isConflict(String ruid, String roomId, Date startTime, Date endTime) {
        // 判断会议是否冲突
        AppointConference ac = new AppointConference();
        ac.setRoomId(roomId);
        ac.setStartTime(startTime);
        ac.setEndTime(endTime);
        ac.setRuid(ruid);

        List<AppointConference> list = appointConferenceMapper.getConflictAppointConferenceList(ac);
        return Objects.nonNull(list) && !list.isEmpty();
    }


    @Override
    public void insert(AppointmentRoomVO params, RpcConnection rpcConnection) {
        String moderatorPassword = params.getModeratorPassword();
        if (StringUtils.isBlank(moderatorPassword)) {
            moderatorPassword = StringUtil.getRandomPassWord(6);
        }

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
        ac.setConferenceMode(params.getConferenceMode().getMode());
        ac.setPassword(params.getPassword());
        ac.setModeratorPassword(moderatorPassword);
        ac.setModeratorName(rpcConnection.getUsername());
        ac.setShortUrl(roomManage.createAppointConferenceShortUrl());
        appointConferenceMapper.insertSelective(ac);

        params.setRuid(ac.getRuid());
    }

    @Override
    public AppointConference getByRuid(String ruid) {
        AppointConferenceExample example = new AppointConferenceExample();
        example.createCriteria().andRuidEqualTo(ruid);
        List<AppointConference> list = appointConferenceMapper.selectByExample(example);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void updateAppointment(AppointConference appt, AppointmentRoomVO vo) {
        appt.setConferenceSubject(vo.getSubject());
        appt.setConferenceDesc(vo.getDesc());
        appt.setStartTime(new Date(vo.getStartTime()));
        appt.setEndTime(new Date(vo.getEndTime()));
        appt.setDuration(vo.getDuration());
        appt.setRoomCapacity(vo.getRoomCapacity());
        appt.setAutoInvite(vo.getAutoCall() ? 1 : 0);
        appt.setConferenceMode(vo.getConferenceMode().getMode());
        appt.setPassword(vo.getPassword());
        appt.setModeratorPassword(vo.getModeratorPassword());
        appointConferenceMapper.updateByPrimaryKey(appt);
    }
}
