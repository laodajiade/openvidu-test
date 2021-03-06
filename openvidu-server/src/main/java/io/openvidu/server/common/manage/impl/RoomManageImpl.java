package io.openvidu.server.common.manage.impl;

import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.*;
import io.openvidu.server.common.enums.ParticipantStatusEnum;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.manage.RoomManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.common.pojo.dto.CorpRoomsSearch;
import io.openvidu.server.core.Participant;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chosongi
 * @date 2020/5/27 16:37
 */
@Slf4j
@Service
public class RoomManageImpl implements RoomManage {

    @Resource
    private ConferenceMapper conferenceMapper;

    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;

    @Resource
    private CorpMcuConfigMapper corpMcuConfigMapper;

    @Resource
    private CacheManage cacheManage;

    @Resource
    private UserManage userManage;

    @Resource
    private AppointConferenceMapper appointConferenceMapper;

    @Resource
    private CorporationMapper corporationMapper;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;


    @Override
    public List<Conference> getAllRoomsOfCorp(CorpRoomsSearch search) {
        return conferenceMapper.selectBySearchParam(search);
    }

    @Override
    public List<ConferencePartHistory> getConfHistoryRecordsByCondition(ConferencePartHistory search) {
        return conferencePartHistoryMapper.selectByCondition(search);
    }

    @Override
    public void storePartHistory(Participant participant, Conference conference) {
        // save part history when stream type is MAJOR and role is not THOR

        //???????????????????????????
        User user = userManage.getUserByUserId(participant.getUserId());

        ConferencePartHistory history = new ConferencePartHistory();
        history.setRuid(conference.getRuid());
        history.setUserId(participant.getUserId());
        history.setUuid(participant.getUuid());
        history.setUsername(user == null ? participant.getUsername() : user.getUsername());
        history.setUserType(participant.getUserType().getType());
        history.setTerminalType(participant.getTerminalType().getDesc());
        history.setStatus(ParticipantStatusEnum.PROCESS.getStatus());
        history.setStartTime(new Date());
        history.setAccessKey(conference.getProject());
        history.setProject(conference.getProject());
        history.setOnlyShare(OpenViduRole.ONLY_SHARE.equals(participant.getRole()) ? 1 : 0);
        conferencePartHistoryMapper.insert(history);

        // save part info in cache
        Map<String, Object> partInfo = new HashMap<>();
        partInfo.put("userId", participant.getUserId());
        partInfo.put("ruid", conference.getRuid());
        partInfo.put("roomId", participant.getSessionId());
        partInfo.put("role", participant.getRole().name());
        partInfo.put("publicId", participant.getParticipantPublicId());
        partInfo.put("shareStatus", participant.getShareStatus().name());
        partInfo.put("speakerStatus", participant.getSpeakerStatus().name());
        partInfo.put("handStatus", participant.getHandStatus().name());
        partInfo.put("micStatus", participant.getMicStatus().name());
        partInfo.put("videoStatus", participant.getVideoStatus().name());
        partInfo.put("order", participant.getOrder());
        cacheManage.savePartInfo(participant.getUuid(), partInfo);
    }

    @Override
    public void updatePartHistory(String ruid, String uuid, Long createdAt) {
        ConferencePartHistory update = new ConferencePartHistory();
        update.setRuid(ruid);
        update.setUuid(uuid);

        update.setStatus(ParticipantStatusEnum.LEAVE.getStatus());
        Date endTime = new Date();
        update.setEndTime(endTime);
        int duration = (int) Math.ceil(((endTime.getTime() - createdAt) * 1.0 / 60000));
        update.setDuration(duration);
        conferencePartHistoryMapper.updatePartHistroy(update);
        Conference conference = conferenceMapper.selectByRuid(ruid);
        if (Objects.nonNull(conference) && !RoomIdTypeEnums.fixed.name().equals(conference.getRoomIdType())) {
            Corporation corporation = corporationMapper.selectByCorpProject(conference.getProject());
            if (Objects.nonNull(corporation)) {
                int remainderDuration = corporation.getRemainderDuration() - duration;
                corporation.setRemainderDuration(remainderDuration);
                corporation.setProject(corporation.getProject());
                corporationMapper.updateCorpRemainderDuration(corporation);

                if (duration == 1) {
                    cacheManage.setCorpRemainDuration(corporation.getProject(), cacheManage.getCorpRemainDuration(corporation.getProject()) - 1);
                }
            }
        }
        cacheManage.delPartInfo(uuid);
    }

    @Override
    public Conference getConferenceByRuid(String ruid) {
        return conferenceMapper.selectByRuid(ruid);
    }

    @Override
    public CorpMcuConfig getCorpMcuConfig(String project) {
        return corpMcuConfigMapper.selectByProject(project);
    }

    @Override
    public int createMeetingRoom(Conference conference) {
        // ??????????????????????????????
        Conference existAppt = null;
        if (conference.getRuid().startsWith("appt-")) {
            existAppt = getConferenceByRuid(conference.getRuid());
        }
        int result = 0;
        if (existAppt == null) {
            result = conferenceMapper.insert(conference);
        }
        if (result > 0) {
            // save in cache
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("ruid", conference.getRuid());
            roomInfo.put("startTime", conference.getStartTime().getTime());
            roomInfo.put("roomCapacity", conference.getRoomCapacity());
            roomInfo.put("password", conference.getPassword());
            roomInfo.put("conferenceSubject", conference.getConferenceSubject());
            roomInfo.put("creatorId", conference.getUserId());
            roomInfo.put("instanceId", instanceId);
            roomInfo.put("roomSubject", conference.getConferenceSubject());
            roomInfo.put("moderatorUuid", conference.getModeratorUuid());

            cacheManage.saveRoomInfo(conference.getRoomId(), roomInfo);
        }

        return result;
    }

    @Override
    public void storeConcurrentNumber(Conference conference) {
        conferenceMapper.updateByPrimaryKeySelective(conference);
    }

    @Override
    public List<String> getSubRoomIds(String roomId, Long orgId) {
        return StringUtils.isNotEmpty(roomId) ? Collections.singletonList(roomId) :
                (Objects.nonNull(orgId) ? userManage.getSubUserByDeptId(orgId).stream().map(User::getUuid).collect(Collectors.toList()) : null);
    }

    @Override
    public List<ConferencePartHistory> getConfRecordDetail(ConferencePartHistory search) {
        return conferencePartHistoryMapper.getConfRecordDetail(search);
    }

    @Override
    public String createShortUrl() {
        String shortUrl = StringUtil.getNonce(8);
        Conference conference = conferenceMapper.getConferenceByShortUrl(shortUrl);
        while (Objects.nonNull(conference)) {
            createShortUrl();
        }
        return shortUrl;
    }

    @Override
    public String createAppointConferenceShortUrl() {
        String shortUrl = StringUtil.getNonce(9);
        AppointConference appointment = appointConferenceMapper.getAppointmentByShortUrl(shortUrl);
        while (Objects.nonNull(appointment)){
            createAppointConferenceShortUrl();
        }
        return shortUrl;
    }


}
