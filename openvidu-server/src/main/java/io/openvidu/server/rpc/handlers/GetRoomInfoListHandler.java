package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.FixedRoomMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.enums.RoomStateEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.FixedRoom;
import io.openvidu.server.common.pojo.vo.ConferenceInfoResp;
import io.openvidu.server.common.pojo.vo.RoomInfoResp;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BeanUtils;
import io.openvidu.server.utils.BindValidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(ProtocolElements.GET_ROOM_INFO_METHOD)
public class GetRoomInfoListHandler extends ExRpcAbstractHandler<JsonObject> {

    @Autowired
    private FixedRoomMapper fixedRoomMapper;


    @Override
    public RespResult<RoomInfoResp> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject jsonObject) {
        BindValidate.notEmpty(jsonObject, "roomId");

        String roomId = getStringParam(request, "roomId");


        RoomIdTypeEnums roomIdType = RoomIdTypeEnums.calculationRoomType(roomId);
        RoomInfoResp roomInfoResp;
        if (roomIdType == RoomIdTypeEnums.fixed) {
            FixedRoom fixedRoom = fixedRoomMapper.selectByRoomId(roomId);
            if (fixedRoom == null) {
                return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            }
            roomInfoResp = BeanUtils.copyToBean(fixedRoom, RoomInfoResp.class);
            roomInfoResp.setCapacity(fixedRoom.getRoomCapacity());
        } else if (rpcConnection.getUserUuid().equals(roomId)) {
            roomInfoResp = new RoomInfoResp();
            roomInfoResp.setRoomId(roomId);
            roomInfoResp.setRoomName(rpcConnection.getUsername() + "的会议");
            Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
            roomInfoResp.setCapacity(corporation.getCapacity());
            roomInfoResp.setActivationDate(corporation.getActivationDate());
            roomInfoResp.setExpireDate(corporation.getExpireDate());
            roomInfoResp.setRoomIdType(RoomIdTypeEnums.personal);
        } else {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }

        Conference conference = conferenceMapper.selectUsedConference(roomId);
        if (conference != null) {
            roomInfoResp.setState(RoomStateEnum.busy);
            ConferenceInfoResp conferenceInfoResp = BeanUtils.copyToBean(conference, ConferenceInfoResp.class);
            roomInfoResp.setConferenceInfo(conferenceInfoResp);
        } else {
            roomInfoResp.setState(RoomStateEnum.idle);
        }

        return RespResult.ok(roomInfoResp);
    }
}
