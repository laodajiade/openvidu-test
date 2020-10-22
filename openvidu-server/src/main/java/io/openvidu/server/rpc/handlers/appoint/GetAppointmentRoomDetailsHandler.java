package io.openvidu.server.rpc.handlers.appoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.exception.BizException;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;

@Slf4j
@Service(ProtocolElements.GET_APPOINTMENT_ROOM_DETAILS_METHOD)
public class GetAppointmentRoomDetailsHandler extends ExRpcAbstractHandler<JsonObject> {

    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Resource
    private ConferenceMapper conferenceMapper;

    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        BindValidate.notEmpty(params, "ruid");

        String ruid = params.get("ruid").getAsString();
        try {
            JsonObject result;
            Detail detail;
            if (ruid.startsWith("appt-")) {
                Conference conference = conferenceMapper.selectByRuid(ruid);
                if (conference != null && conference.getStatus() == ConferenceStatus.FINISHED.getStatus()) {
                    detail = new ConferenceDetail();
                } else {
                    detail = new AppointmentDetail();
                }
            } else {
                detail = new ConferenceDetail();
            }
            result = detail.getDetail(rpcConnection, ruid);
            return RespResult.ok(result);
        } catch (BizException e) {
            return RespResult.fail(e.getRespEnum());
        }
    }


    class AppointmentDetail implements Detail {
        public JsonObject getDetail(RpcConnection rpcConnection, String ruid) {
            AppointConference appointConference = appointConferenceManage.getByRuid(ruid);

            if (appointConference == null) {
                throw new BizException(ErrorCodeEnum.CONFERENCE_RECORD_NOT_EXIST);
            }

            // 根据ruid获取所有的会议邀请信息
            List<User> parts = conferencePartHistoryMapper.selectUserByRuid(ruid);

            if (rpcConnection.getAccessType()!= AccessTypeEnum.web) {
                if (parts.stream().noneMatch(user -> user.getId().equals(rpcConnection.getUserId())) && !appointConference.getUserId().equals(rpcConnection.getUserId())) {
                    throw new BizException(ErrorCodeEnum.CONFERENCE_RECORD_NOT_EXIST);
                }
            }

            // 获取到所有预约会议发起人对象
            User creator = userManage.getUserByUserId(appointConference.getUserId());

            // 封装响应参数
            return getAppointConfJson(appointConference, parts, creator);
        }

        private JsonObject getAppointConfJson(AppointConference appointConference, List<User> parts, User creator) {
            JsonObject appointConfObj = new JsonObject();
            appointConfObj.addProperty("ruid", appointConference.getRuid());
            appointConfObj.addProperty("roomId", appointConference.getRoomId());
            appointConfObj.addProperty("subject", appointConference.getConferenceSubject());
            appointConfObj.addProperty("conferenceMode", ConferenceModeEnum.parse(appointConference.getConferenceMode()).name());
            appointConfObj.addProperty("autoCall", appointConference.getAutoInvite().compareTo(0) == 0);// 参数设置
            appointConfObj.addProperty("roomCapacity", appointConference.getRoomCapacity());
            appointConfObj.addProperty("startTime", String.valueOf(appointConference.getStartTime().getTime()));
            appointConfObj.addProperty("duration", appointConference.getDuration());
            appointConfObj.addProperty("desc", appointConference.getConferenceDesc());
            appointConfObj.addProperty("moderatorRoomId", appointConference.getModeratorUuid());
            // 会议发起人
            appointConfObj.addProperty("createorUsername", Objects.nonNull(creator) ? creator.getUsername() : "");
            appointConfObj.addProperty("createorAccount", Objects.nonNull(creator) ? creator.getUuid() : "");
            appointConfObj.addProperty("creatorUserIcon", Objects.nonNull(creator) ? creator.getIcon() : "");

            appointConfObj.addProperty("isStart", appointConference.getStartTime().before(new Date()));
            appointConfObj.addProperty("project", appointConference.getProject());
            appointConfObj.addProperty("password", appointConference.getPassword());
            appointConfObj.addProperty("moderatorPassword", appointConference.getModeratorPassword());

            appointConfObj.add("participants", constructAppointPartsInfo(parts));

            return appointConfObj;
        }


        private JsonArray constructAppointPartsInfo(List<User> parts) {
            JsonArray partInfoArr = new JsonArray();

            for (User user : parts) {
                JsonObject partInfo = new JsonObject();
                partInfo.addProperty("account", user.getUuid());
                partInfo.addProperty("username", user.getUsername());
                partInfo.addProperty("userIcon", "");

                partInfoArr.add(partInfo);
            }

            return partInfoArr;
        }
    }

    class ConferenceDetail implements Detail {

        @Override
        public JsonObject getDetail(RpcConnection rpcConnection, String ruid) {
            Conference conference = conferenceMapper.selectByRuid(ruid);

            if (conference == null) {
                throw new BizException(ErrorCodeEnum.CONFERENCE_RECORD_NOT_EXIST);
            }

            // 根据ruid获取所有参会者列表
            List<ConferencePartHistory> conferencePartHistories = conferencePartHistoryMapper.selectConfPartHistoryByRuids(singletonList(ruid));

            // 获取到所有预约会议发起人对象
            User creator = userManage.getUserByUserId(conference.getUserId());

            // 封装响应参数
            return translateResultJson(conference, conferencePartHistories, creator);
        }

        private JsonObject translateResultJson(Conference conference, List<ConferencePartHistory> participants, User creator) {
            JsonObject appointConfObj = new JsonObject();
            appointConfObj.addProperty("ruid", conference.getRuid());
            appointConfObj.addProperty("roomId", conference.getRoomId());
            appointConfObj.addProperty("subject", conference.getConferenceSubject());
            appointConfObj.addProperty("conferenceMode", ConferenceModeEnum.parse(conference.getConferenceMode()).name());
            appointConfObj.addProperty("roomCapacity", conference.getRoomCapacity());
            appointConfObj.addProperty("startTime", String.valueOf(conference.getStartTime().getTime()));
            appointConfObj.addProperty("desc", conference.getConferenceDesc());
            appointConfObj.addProperty("moderatorRoomId", conference.getModeratorUuid());
            // 会议发起人
            appointConfObj.addProperty("createorUsername", Objects.nonNull(creator) ? creator.getUsername() : "");
            appointConfObj.addProperty("createorAccount", Objects.nonNull(creator) ? creator.getUuid() : "");
            appointConfObj.addProperty("creatorUserIcon", Objects.nonNull(creator) ? creator.getIcon() : "");
            appointConfObj.addProperty("isStart", conference.getStartTime().before(new Date()));
            appointConfObj.addProperty("project", conference.getProject());
            appointConfObj.addProperty("password", conference.getPassword());
            appointConfObj.addProperty("moderatorPassword", conference.getModeratorPassword());

            appointConfObj.add("participants", constructAppointPartsInfo(participants));

            if (conference.getRuid().startsWith("appt-")) {
                appointConfObj.addProperty("autoCall", appointConferenceManage.getByRuid(conference.getRuid()).getAutoInvite() == 1);
            } else {
                appointConfObj.addProperty("autoCall", false);
            }
            appointConfObj.addProperty("duration", (conference.getEndTime().getTime() - conference.getStartTime().getTime()) / 60000);


            return appointConfObj;
        }

        private JsonArray constructAppointPartsInfo(List<ConferencePartHistory> partHistories) {
            JsonArray partInfoArr = new JsonArray();

            for (ConferencePartHistory partHistory : partHistories) {
                JsonObject partInfo = new JsonObject();
                partInfo.addProperty("account", partHistory.getUuid());
                partInfo.addProperty("username", partHistory.getUsername());
                partInfo.addProperty("userIcon", "");
                partInfoArr.add(partInfo);
            }

            return partInfoArr;
        }


    }

    interface Detail {
        JsonObject getDetail(RpcConnection rpcConnection, String ruid);
    }
}


