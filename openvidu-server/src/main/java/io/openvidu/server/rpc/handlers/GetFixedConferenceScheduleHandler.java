package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.core.PageResult;
import io.openvidu.server.domain.vo.FixEDRoomScheduleVO;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: prepaid-platform
 * @description: 查询固定会议室日程
 * @author: WuBing
 * @create: 2021-09-29 14:29
 **/
@Service
@Slf4j
public class GetFixedConferenceScheduleHandler extends RpcAbstractHandler {

    @Autowired
    AppointConferenceMapper appointConferenceMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        try {
            String roomId = getStringParam(request, ProtocolElements.GET_FIXED_CONFERENCE_SCHEDULE_ROOMID_PARAM);
            int pagenum = getIntParam(request, ProtocolElements.PAGENUM);
            int pagesize = getIntParam(request, ProtocolElements.PAGESIZE);
            PageResult<FixEDRoomScheduleVO> fixEDRoomScheduleVOPageResult = pendingAboutAppointment(roomId, pagenum, pagesize);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), fixEDRoomScheduleVOPageResult);
        } catch (IllegalArgumentException e) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }catch (Exception e){
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
            return;
        }

    }


    protected PageResult<FixEDRoomScheduleVO> pendingAboutAppointment(String roomId, int pageNum, int pageSize) {
        Page<Object> page = PageHelper.startPage(pageNum, pageSize);
        List<AppointConference> appointConferences = appointConferenceMapper.pendingAndStartfiexdRoomSchedule(roomId);
        return new PageResult(transferResp(appointConferences), page);
    }

    private List<FixEDRoomScheduleVO> transferResp(List<AppointConference> appointConferenceList) {
        List<FixEDRoomScheduleVO> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(appointConferenceList)) {
            return list;
        }
        for (AppointConference appointConference : appointConferenceList) {
            FixEDRoomScheduleVO resp = new FixEDRoomScheduleVO();
            resp.setRuid(appointConference.getRuid());
            resp.setRoomId(appointConference.getRoomId());
            resp.setSubject(appointConference.getConferenceSubject());
            resp.setStartTime(appointConference.getStartTime().getTime() + "");
            resp.setDuration(appointConference.getDuration());
            resp.setDesc(appointConference.getConferenceDesc());
            resp.setCreatorUsername(appointConference.getModeratorName());
            resp.setCreatorAccount(appointConference.getModeratorUuid());
            resp.setEndTime(appointConference.getEndTime() == null ? "" : (appointConference.getEndTime().getTime() + ""));
            resp.setStatus(appointConference.getStatus());
            list.add(resp);
        }

        return list;
    }
}
