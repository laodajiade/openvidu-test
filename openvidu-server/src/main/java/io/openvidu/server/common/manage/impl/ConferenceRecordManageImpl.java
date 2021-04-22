package io.openvidu.server.common.manage.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.dao.ConferenceRecordInfoMapper;
import io.openvidu.server.common.dao.ConferenceRecordMapper;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.dao.RoomRecordSummaryMapper;
import io.openvidu.server.common.enums.ConferenceRecordStatusEnum;
import io.openvidu.server.common.enums.RecordState;
import io.openvidu.server.common.enums.YesNoEnum;
import io.openvidu.server.common.kafka.RecordingKafkaProducer;
import io.openvidu.server.common.manage.ConferenceRecordManage;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.common.redis.RecordingRedisPublisher;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.core.KurentoSession;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.MediaEvent;
import org.kurento.client.RecordingEvent;
import org.kurento.client.StoppedEvent;
import org.kurento.client.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Service
@Slf4j
public class ConferenceRecordManageImpl implements ConferenceRecordManage {
    @Resource
    private ConferenceRecordMapper conferenceRecordMapper;

    @Resource
    private ConferenceRecordInfoMapper conferenceRecordInfoMapper;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private RoomRecordSummaryMapper recordSummaryMapper;

    @Resource
    private RecordingKafkaProducer recordingKafkaProducer;

    @Resource
    private RecordingRedisPublisher recordingRedisPublisher;

    @Resource
    private CorporationMapper corporationMapper;

    @Value("${recording.path}")
    private String recordingPath;

    @Override
    public int deleteByPrimaryKey(Long id) {
        return conferenceRecordMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(ConferenceRecord record) {
        return conferenceRecordMapper.insert(record);
    }

    @Override
    public int insertSelective(ConferenceRecord record) {
        return conferenceRecordMapper.insertSelective(record);
    }

    @Override
    public ConferenceRecord selectByPrimaryKey(Long id) {
        return conferenceRecordMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(ConferenceRecord record) {
        return conferenceRecordMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(ConferenceRecord record) {
        return conferenceRecordMapper.updateByPrimaryKey(record);
    }

    @Override
    public List<ConferenceRecord> getByCondition(ConferenceRecord record) {
        return conferenceRecordMapper.getByCondition(record);
    }

    @Override
    @Transactional
    public void dealConfRecordEvent(KurentoSession session, MediaEvent event) {
        if (event instanceof RecordingEvent) {
            log.info("dealConfRecordEvent RecordingEvent");
            startConferenceRecord(session, event);
        } else if (event instanceof StoppedEvent) {
            RecordState recordState;
            List<Tag> tags = event.getTags();
            log.info("dealConfRecordEvent StoppedEvent tags size:{}", tags.size());
            if (!CollectionUtils.isEmpty(tags)) {
                String state = tags.stream().filter(tag -> "recordState".equals(tag.getKey())).findAny().map(Tag::getValue).orElse("3");
                log.info("dealConfRecordEvent StoppedEvent state:{}", state);
                recordState = RecordState.parseRecState(Integer.parseInt(state));
                switch (recordState) {
                    case RECORDING:
                        // insert into rec info
                        conferenceRecordInfoMapper.insert(constructForConfRecInfo(session, event));
                        conferenceRecordMapper.increaseConferenceRecordCountByRuid(session.getRuid());
                        break;
                    case RECORD_STOP:
                    case RECORD_EXCEPTION:
                        // query ever exists partition rec
                        List<ConferenceRecordInfo> conferenceRecordInfoList = conferenceRecordInfoMapper.selectByRuid(session.getRuid());
                        // insert rec info and update rec summary
                        updateConfRec(conferenceRecordInfoList, session, event);
                        break;
                }
            }
        }
    }

    private void updateConfRec(List<ConferenceRecordInfo> conferenceRecordInfoList, KurentoSession session, MediaEvent event) {
        ConferenceRecord update = new ConferenceRecord();
        update.setRuid(session.getRuid());
        update.setStatus(ConferenceRecordStatusEnum.FINISH.getStatus());
        ConferenceRecordInfo insert = constructForConfRecInfo(session, event);
        insert.setIsLast(YesNoEnum.Y.name());
        if (!CollectionUtils.isEmpty(conferenceRecordInfoList)) {
            update.setTotalDuration((conferenceRecordInfoList.stream().mapToInt(ConferenceRecordInfo::getDuration).sum() + insert.getDuration()));
        } else {
            update.setTotalDuration(insert.getDuration());
        }

        conferenceRecordInfoMapper.insert(insert);
        conferenceRecordMapper.updateByRuid(update);
        sessionManager.setStopRecordingTime(session.getSessionId(), Long.valueOf(event.getTimestampMillis()));
    }

    private ConferenceRecordInfo constructForConfRecInfo(KurentoSession session, MediaEvent event) {
        String fileName = "", thumbUri = null;
        long fileSize = 0L, fileStartTime = 0L, fileEndTime = 0L;
        int recordState = 0;
        for (Tag tag : event.getTags()) {
            log.info("MediaEvent tags: key={}, value={}", tag.getKey(), tag.getValue());
            switch (tag.getKey()) {
                case "fileName":
                    fileName = tag.getValue();
                    break;
                case "fileSize":
                    fileSize = Long.parseLong(tag.getValue());
                    break;
                case "fileStartTime":
                    fileStartTime = Long.valueOf(tag.getValue());
                    break;
                case "fileEndTime":
                    fileEndTime = Long.valueOf(tag.getValue());
                    break;
                case "recordState":
                    recordState = Integer.parseInt(tag.getValue());
                    break;
                case "thumbUri":
                    thumbUri = tag.getValue();
                    break;
            }
        }

        String recordName = fileName.substring(fileName.lastIndexOf("/") + 1);
        ConferenceRecordInfo conferenceRecordInfo = new ConferenceRecordInfo();
        conferenceRecordInfo.setRuid(session.getRuid());
        conferenceRecordInfo.setDuration(Integer.parseInt(String.valueOf(
                (fileEndTime - fileStartTime) / 60000)));
        conferenceRecordInfo.setStartTime(new Date(fileStartTime));
        conferenceRecordInfo.setEndTime(new Date(fileEndTime));
        conferenceRecordInfo.setIsLast(YesNoEnum.N.name());
        conferenceRecordInfo.setRecordName(recordName);
        conferenceRecordInfo.setRecordDisplayName(getDisplayName(event));
        conferenceRecordInfo.setRecordUrl(fileName);
        conferenceRecordInfo.setRecordSize(fileSize);
        conferenceRecordInfo.setThumbnailUrl(thumbUri);
        conferenceRecordInfo.setStatus(recordState != RecordState.RECORD_EXCEPTION.getState() ? YesNoEnum.Y.name() : YesNoEnum.N.name());

        return conferenceRecordInfo;
    }

    private void startConferenceRecord(KurentoSession session, MediaEvent event) {
        ConferenceRecord update = new ConferenceRecord();
        update.setRuid(session.getRuid());
        update.setStatus(ConferenceRecordStatusEnum.PROCESS.getStatus());
        update.setRequestStartTime(new Date(Long.valueOf(event.getTimestampMillis())));
        conferenceRecordMapper.updateByRuidSelective(update);
    }

    private String getDisplayName(MediaEvent event) {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(Long.valueOf(event.getTimestampMillis())));
    }

    @Override
    public Page<RoomRecordSummary> getRoomRecordSummaryByCondition(ConferenceRecordSearch search) {
        return PageHelper.startPage(search.getPageNum(), search.getSize())
                .doSelectPage(() -> recordSummaryMapper.selectByCondition(search));
    }

    @Override
    @Transactional
    public void clearRoomRecords(String roomId, List<String> ruids, String project) {
        recordSummaryMapper.deleteByRoomId(roomId);
        conferenceRecordMapper.deleteByRoomId(roomId);
        conferenceRecordInfoMapper.deleteByRuids(ruids);

        // send delete file path producer task
        JsonObject object = new JsonObject();
        object.addProperty("filePath", getRoomRecordAbsolutePath(project, roomId));
        JsonObject params = new JsonObject();
        params.addProperty("method", CommonConstants.MQ_METHOD_DEL_RECORDING_FILE);
        params.add("params", object);

        recordingRedisPublisher.sendRecordingFileOperationTask(params.toString());

    }

    @Override
    @Transactional
    public void deleteConferenceRecord(List<ConferenceRecordInfo> conferenceRecordInfos) {
        JsonArray fileArr = new JsonArray();
        conferenceRecordInfos.forEach(conferenceRecordInfo -> {
            ConferenceRecord search = new ConferenceRecord();
            search.setRuid(conferenceRecordInfo.getRuid());
            ConferenceRecord conferenceRecord = conferenceRecordMapper.getByCondition(search).get(0);

            // 删除录制会议详情
            conferenceRecordInfoMapper.deleteByPrimaryKey(conferenceRecordInfo.getId());
            // 修改录制会议文件数量
            conferenceRecordMapper.decreaseConferenceRecordCountByRuid(conferenceRecordInfo.getRuid());
            // 修改房间概览文件存储量
            RoomRecordSummary update = new RoomRecordSummary();
            update.setRoomId(conferenceRecord.getRoomId());
            update.setOccupation(conferenceRecordInfo.getRecordSize());
            recordSummaryMapper.decreaseRecordSummary(update);

            fileArr.add(conferenceRecordInfo.getRecordUrl());
        });

        // 删除所有录制完成且recordCount为0的录制会议
        conferenceRecordMapper.deleteUselessRecord();
        // 删除所有房间录制概览记录
        recordSummaryMapper.deleteUselessSummaryInfo();

        // send delete files producer task
        JsonObject filesObj = new JsonObject();
        filesObj.add("files", fileArr);
        JsonObject params = new JsonObject();
        params.addProperty("method", CommonConstants.MQ_METHOD_DEL_RECORDING_FILE);
        params.add("params", filesObj);

        recordingRedisPublisher.sendRecordingFileOperationTask(params.toString());
    }

    @Override
    public List<RoomRecordSummary> getAllRoomRecordSummaryByProject(ConferenceRecordSearch search) {
        return recordSummaryMapper.selectByCondition(search);
    }

    @Override
    public void updatePreRecordErrorStatus(ConferenceRecord record) {
        conferenceRecordMapper.updatePreRecordErrorStatus(record);
    }

    private String getRoomRecordAbsolutePath(String project, String roomId) {
        return new StringBuilder(recordingPath)
                .append(project)
                .append("/")
                .append(roomId)
                .append("/").toString();
    }

    @Override
    public BigDecimal getCorpRecordStorage(String project) {
        Corporation corporation = corporationMapper.selectByCorpProject(project);
        return new BigDecimal(Objects.nonNull(corporation) && Objects.nonNull(corporation.getRecordingCapacity()) ?
                corporation.getRecordingCapacity() * 1024 : 0).setScale(2, RoundingMode.UP);
    }

    @Override
    public ConferenceRecord getByRuIdRecordStatus(String ruId) {
        return conferenceRecordMapper.getByRuIdRecordStatus(ruId);
    }
}
