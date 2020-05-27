package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;

import java.util.List;

/**
 * @author chosongi
 * @date 2020/5/27 16:37
 */
public interface RoomManage {
    List<Conference> getAllRoomsOfCorp(ConferenceSearch allRoomsOfCropSearch);
}
