package io.openvidu.server.kurento.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ModeratorLayoutInfo {

    private String roomId;
    @Getter
    private LayoutModeTypeEnum layoutModeType = LayoutModeTypeEnum.NORMAL;
    private long timestamp = 0;
    @Getter
    private LayoutModeEnum mode = LayoutModeEnum.ONE;

    private Session session;

    @Getter
    private Layout layout;

    @Getter
    private String moderatorDeviceModel;

    @Getter
    private TerminalTypeEnum moderatorTerminalType;

    /**
     * 0 = 主持人模式，1 = 主动模式
     */
    @Getter
    private boolean autoMode = true;

    public ModeratorLayoutInfo(Session session) {
        this.session = session;
        this.roomId = session.getSessionId();
    }

    public void switchAutoMode() {
        this.autoMode = true;
        timestamp = 0;
        layoutModeType = LayoutModeTypeEnum.NORMAL;
        LayoutModeEnum mode = LayoutModeEnum.ONE;
    }

    public void updateLayout(long timestamp, LayoutModeEnum mode, LayoutModeTypeEnum layoutModeType,
                             JsonArray layoutJsonArray, Participant moderatorPart) {
        this.autoMode = false;

        if (timestamp < this.timestamp) {
            return;
        }
        this.timestamp = timestamp;
        this.layoutModeType = layoutModeType;
        this.mode = mode;

        this.moderatorDeviceModel = moderatorPart.getDeviceModel();
        this.moderatorTerminalType = moderatorPart.getTerminalType();

        Layout layout = new Layout();
        for (JsonElement jsonElement : layoutJsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String uuid = jsonObject.get("uuid").getAsString();
            String streamType = jsonObject.get("streamType").getAsString();

            layout.items.add(new Item(uuid, StreamType.valueOf(streamType)));
        }

        this.layout = layout;
        if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
            session.getCompositeService().asyncUpdateComposite();
        }
        if (session.getIsRecording()) {
            //todo 2.0.1 yy 录制更新布局
            //session.getRecorderService().asyncUpdateComposite();
        }


    }

    static class Layout {
        List<Item> items = new ArrayList<>();
    }

    static class Item {
        String uuid;
        StreamType streamType;

        public Item(String uuid, StreamType streamType) {
            this.uuid = uuid;
            this.streamType = streamType;
        }
    }
}
