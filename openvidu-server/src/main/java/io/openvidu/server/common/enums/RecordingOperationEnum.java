package io.openvidu.server.common.enums;

import com.google.gson.JsonObject;
import io.openvidu.server.kurento.core.ConferenceRecordingProperties;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * @author chosongi
 * @date 2020/9/8 16:06
 */
public enum RecordingOperationEnum {
    startRecording {
        @Override
        public JsonObject buildMqMsg(ConferenceRecordingProperties recordingProperties) {
            JsonObject params = new JsonObject();
            params.addProperty("project", recordingProperties.getProject());
            params.addProperty("roomId", recordingProperties.getRoomId());
            params.addProperty("ruid", recordingProperties.getRuid());
            params.addProperty("startTime", recordingProperties.getStartTime());
            params.addProperty("updateTime", recordingProperties.getUpdateTime());
            params.addProperty("rootPath", recordingProperties.getRootPath());
            params.addProperty("layoutMode", recordingProperties.getLayoutMode());
            params.addProperty("layoutModeType", recordingProperties.getLayoutModeTypeEnum().name());
            params.addProperty("outPutMode", recordingProperties.getOutputMode().name());
            params.addProperty("mediaProfileSpecType", recordingProperties.getMediaProfileSpecType().name());
            params.addProperty("serialId", RandomStringUtils.randomAlphabetic(6));
            params.add("mediaSources", recordingProperties.getMediaSources());

            return wrapperMsg(params);
        }
    },

    stopRecording {
        @Override
        public JsonObject buildMqMsg(ConferenceRecordingProperties recordingProperties) {
            JsonObject params = new JsonObject();
            params.addProperty("ruid", recordingProperties.getRuid());
            params.addProperty("outPutMode", recordingProperties.getOutputMode().name());

            return wrapperMsg(params);
        }
    },

    updateRecording {
        @Override
        public JsonObject buildMqMsg(ConferenceRecordingProperties recordingProperties) {
            JsonObject params = new JsonObject();
            params.addProperty("ruid", recordingProperties.getRuid());
            params.addProperty("outPutMode", recordingProperties.getOutputMode().name());
            params.addProperty("layoutMode", recordingProperties.getLayoutMode());
            params.add("mediaSources", recordingProperties.getMediaSources());

            return wrapperMsg(params);
        }
    };

    public abstract JsonObject buildMqMsg(ConferenceRecordingProperties recordingProperties);

    JsonObject wrapperMsg(JsonObject params) {
        JsonObject msg = new JsonObject();
        msg.addProperty("method", this.name());
        msg.add("params", params);

        return msg;
    }
}
