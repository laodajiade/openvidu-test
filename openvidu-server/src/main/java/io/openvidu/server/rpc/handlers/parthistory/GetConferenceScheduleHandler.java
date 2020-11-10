package io.openvidu.server.rpc.handlers.parthistory;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.core.PageResult;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.vo.ConferenceHisResp;
import io.openvidu.server.domain.vo.GetConferenceScheduleVO;
import io.openvidu.server.exception.BindValidateException;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service(ProtocolElements.GET_CONFERENCE_SCHEDULE_METHOD)
public class GetConferenceScheduleHandler extends ExRpcAbstractHandler<GetConferenceScheduleVO> {

    @Resource
    ConferenceMapper conferenceMapper;

    @Autowired
    AppointConferenceMapper appointConferenceMapper;


    @Override
    public RespResult<PageResult<ConferenceHisResp>> doProcess(RpcConnection rpcConnection, Request<GetConferenceScheduleVO> request,
                                                               GetConferenceScheduleVO params) {
        BindValidate.notNull(params::getStatus);
        BindValidate.notNull(params::getPageNum);
        BindValidate.notNull(params::getPageSize);

        List<ConferenceHisResp> conferences;

        Page<Object> page = PageHelper.startPage(params.getPageNum(), params.getPageSize());

        if ("pending".equals(params.getStatus())) {
            conferences = pending(rpcConnection, params);
        } else {
            conferences = new Finished(rpcConnection.getUserId(), params).getList();
        }

        fetchUpCreator(conferences);
        return RespResult.ok(new PageResult<>(conferences, page));
    }

    protected List<ConferenceHisResp> pending(RpcConnection rpcConnection, GetConferenceScheduleVO params) {
        return new Pending(rpcConnection.getUserId(), rpcConnection.getProject(), params).getList();
    }


    /**
     * 补全创建人
     */
    protected void fetchUpCreator(List<ConferenceHisResp> list) {

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        List<Long> userIdSet = list.stream().map(ConferenceHisResp::getCreatorUserId).distinct().collect(Collectors.toList());

        List<UserDeviceDeptInfo> users = userMapper.queryUserInfoByUserIds(userIdSet);

        Map<Long, UserDeviceDeptInfo> userMap = users.stream().collect(Collectors.toMap(UserDeviceDeptInfo::getUserId, Function.identity()));

        for (ConferenceHisResp conferenceHisResp : list) {
            UserDeviceDeptInfo user = userMap.get(conferenceHisResp.getCreatorUserId());
            if (user != null) {
                conferenceHisResp.setCreatorAccount(user.getUuid());
                conferenceHisResp.setCreatorUsername(StringUtils.isNotBlank(user.getUsername()) ? user.getUsername() : user.getDeviceName());
                conferenceHisResp.setCreatorUserIcon("");
            } else {
                conferenceHisResp.setCreatorAccount("");
                conferenceHisResp.setCreatorUsername("已注销");
                conferenceHisResp.setCreatorUserIcon("");
            }
        }
    }


    class Pending {
        Long userId;
        String project;
        GetConferenceScheduleVO vo;

        public Pending(Long userId, String projcet, GetConferenceScheduleVO vo) {
            this.vo = vo;
            this.project = projcet;
            this.userId = userId;
        }

        public List<ConferenceHisResp> getList() {
            return pendingAboutAppointment();
        }

        private List<ConferenceHisResp> pendingAboutAppointment() {
            AppointConference appointConference = new AppointConference();
            appointConference.setUserId(userId);
            appointConference.setProject(this.project);

            if (StringUtils.isNotBlank(vo.getDate())) {
                try {
                    appointConference.setStartTime(DateUtils.parseDate(vo.getDate() + " 00:00:00", "yyyy-MM-dd HH:mm:ss"));
                    appointConference.setEndTime(DateUtils.parseDate(vo.getDate() + " 23:59:59", "yyyy-MM-dd HH:mm:ss"));
                } catch (ParseException e) {
                    log.error("date parse error error", e);
                    throw new BindValidateException("date parse error date=" + vo.getDate());
                }
            }


            List<AppointConference> appointConferenceList = appointConferenceMapper.pendingAboutAppointment(appointConference);


            return transferResp(appointConferenceList);
        }

        private List<ConferenceHisResp> transferResp(List<AppointConference> appointConferenceList) {
            List<ConferenceHisResp> list = new ArrayList<>();
            if (CollectionUtils.isEmpty(appointConferenceList)) {
                return list;
            }


            for (AppointConference appointConference : appointConferenceList) {
                ConferenceHisResp resp = new ConferenceHisResp();

                resp.setRuid(appointConference.getRuid());
                resp.setRoomId(appointConference.getRoomId());
                resp.setSubject(appointConference.getConferenceSubject());
                resp.setDesc(appointConference.getConferenceDesc());
                resp.setModeratorUuid(appointConference.getModeratorUuid());
                resp.setStartTime(appointConference.getStartTime().getTime() + "");
                resp.setEndTime((appointConference.getEndTime().getTime() + ""));
                resp.setDuration(appointConference.getDuration());
                resp.setRoomCapacity(appointConference.getRoomCapacity());
                resp.setCreatorUserId(appointConference.getUserId());

                list.add(resp);
            }

            return list;
        }
    }

    class Finished {

        Long userId;
        GetConferenceScheduleVO vo;

        public Finished(Long userId, GetConferenceScheduleVO vo) {
            this.vo = vo;
            this.userId = userId;
        }

        public List<ConferenceHisResp> getList() {
            return finishedAboutConference();
        }

        private List<ConferenceHisResp> finishedAboutConference() {
            List<Conference> conferences = conferenceMapper.getFinishedList(userId);
            return transferResp(conferences);
        }

        private List<ConferenceHisResp> transferResp(List<Conference> appointConferenceList) {
            List<ConferenceHisResp> list = new ArrayList<>();
            if (CollectionUtils.isEmpty(appointConferenceList)) {
                return list;
            }

            for (Conference conference : appointConferenceList) {
                ConferenceHisResp resp = new ConferenceHisResp();

                resp.setRuid(conference.getRuid());
                resp.setRoomId(conference.getRoomId());
                resp.setSubject(conference.getConferenceSubject());
                resp.setDesc(conference.getConferenceDesc());
                resp.setModeratorUuid(conference.getModeratorUuid());
                resp.setStartTime(String.valueOf(conference.getStartTime().getTime()));
                resp.setEndTime(conference.getEndTime() != null ? String.valueOf(conference.getEndTime().getTime()) : null);
                // resp.setDuration(conference.getDuration()); 暂时没有
                resp.setRoomCapacity(conference.getRoomCapacity());
                resp.setCreatorUserId(conference.getUserId());
                list.add(resp);
            }

            return list;
        }

    }
}
