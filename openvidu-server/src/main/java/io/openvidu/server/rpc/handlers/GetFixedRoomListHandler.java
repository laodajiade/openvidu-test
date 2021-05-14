package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.FixedRoomMapper;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.enums.RoomStateEnum;
import io.openvidu.server.common.pojo.FixedRoom;
import io.openvidu.server.common.pojo.vo.FixedRoomResp;
import io.openvidu.server.core.PageResult;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.vo.PageVO;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BeanUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service(ProtocolElements.GET_FIXED_ROOM_LIST_METHOD)
public class GetFixedRoomListHandler extends ExRpcAbstractHandler<PageVO> {

    @Autowired
    private FixedRoomMapper fixedRoomMapper;


    @Override
    public RespResult<PageResult<FixedRoomResp>> doProcess(RpcConnection rpcConnection, Request<PageVO> request, PageVO params) {
        Page<Object> page = PageHelper.startPage(params.getPageNum(), params.getPageSize());
        if (params.getIsChooseAll()) {
            page = PageHelper.startPage(1, Integer.MAX_VALUE);
        }
        List<FixedRoom> fixedRoomList = fixedRoomMapper.getFixedRoomList(rpcConnection.getUserId(), rpcConnection.getCorpId());

        List<FixedRoomResp> fixedRoomResps = fixedRoomList.stream().map(fixedRoom -> {
            FixedRoomResp resp = BeanUtils.copyToBean(fixedRoom, FixedRoomResp.class);
            resp.setCapacity(fixedRoom.getRoomCapacity());
            resp.setRoomIdType(RoomIdTypeEnums.fixed);
            resp.setState(conferenceMapper.selectUsedConference(fixedRoom.getRoomId()) == null ? RoomStateEnum.idle : RoomStateEnum.busy);
            return resp;
        }).collect(Collectors.toList());


        return RespResult.ok(new PageResult<>(fixedRoomResps, page));
    }
}
