package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.manage.RoomManage;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chosongi
 * @date 2020/5/27 16:37
 */
@Service
public class RoomManageImpl implements RoomManage {
    @Resource
    private ConferenceMapper conferenceMapper;


    @Override
    public List<Conference> getAllRoomsOfCorp(ConferenceSearch conferenceSearch) {
        return conferenceMapper.selectBySearchCondition(conferenceSearch);
    }
}
