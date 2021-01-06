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
import io.openvidu.server.domain.AppointConferenceDTO;
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
import java.util.Collection;
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

        PageResult<ConferenceHisResp> result = getInstance(rpcConnection, params).getList();
        fetchUpCreator(result.getList());
        return RespResult.ok(result);
    }

    protected ISchedule getInstance(RpcConnection rpcConnection, GetConferenceScheduleVO params) {
        if ("pending".equals(params.getStatus())) {
            return new Pending(rpcConnection.getUserId(), rpcConnection.getProject(), params);
        } else {
            return new Finished(rpcConnection.getUserId(), params);
        }
    }


    /**
     * 补全创建人
     */
    private void fetchUpCreator(Collection<ConferenceHisResp> list) {

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

    interface ISchedule {
        PageResult<ConferenceHisResp> getList();
    }

    class Pending implements ISchedule {
        Long userId;
        String project;
        GetConferenceScheduleVO vo;

        public Pending(Long userId, String project, GetConferenceScheduleVO vo) {
            this.vo = vo;
            this.project = project;
            this.userId = userId;
        }

        @Override
        public PageResult<ConferenceHisResp> getList() {
            AppointConferenceDTO appointConference = getAppointConferenceDTO();
            Page<Object> page = PageHelper.startPage(vo.getPageNum(), vo.getPageSize());
            List<AppointConference> appointConferenceList = pendingAboutAppointment(appointConference);
            return new PageResult<>(transferResp(appointConferenceList), page);
        }

        protected AppointConferenceDTO getAppointConferenceDTO() {
            AppointConferenceDTO appointConference = new AppointConferenceDTO();
            appointConference.setUserId(userId);
            appointConference.setProject(this.project);
            appointConference.setOnlyCreator(vo.getOnlyCreator() != null && vo.getOnlyCreator());

            if (StringUtils.isNotBlank(vo.getDate())) {
                try {
                    appointConference.setStartTime(DateUtils.parseDate(vo.getDate() + " 00:00:00", "yyyy-MM-dd HH:mm:ss"));
                    appointConference.setEndTime(DateUtils.parseDate(vo.getDate() + " 23:59:59", "yyyy-MM-dd HH:mm:ss"));
                } catch (ParseException e) {
                    log.error("date parse error error", e);
                    throw new BindValidateException("date parse error date=" + vo.getDate());
                }
            }
            return appointConference;
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
                resp.setEndTime(appointConference.getEndTime() == null ? "" : (appointConference.getEndTime().getTime() + ""));
                resp.setDuration(appointConference.getDuration());
                resp.setRoomCapacity(appointConference.getRoomCapacity());
                resp.setCreatorUserId(appointConference.getUserId());
                resp.setStatus(appointConference.getStatus());

                list.add(resp);
            }

            return list;
        }

        protected List<AppointConference> pendingAboutAppointment(AppointConferenceDTO appointConference) {
            return appointConferenceMapper.pendingAboutAppointment(appointConference);
        }
    }

    class Finished implements ISchedule {

        Long userId;
        GetConferenceScheduleVO vo;

        public Finished(Long userId, GetConferenceScheduleVO vo) {
            this.vo = vo;
            this.userId = userId;
        }

        @Override
        public PageResult<ConferenceHisResp> getList() {
            Page<Object> page = PageHelper.startPage(vo.getPageNum(), vo.getPageSize());
            List<ConferenceHisResp> list = finishedAboutConference();
            return new PageResult<>(list, page);
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
                resp.setStatus(conference.getStatus());
                list.add(resp);
            }

            return list;
        }

    }
}
