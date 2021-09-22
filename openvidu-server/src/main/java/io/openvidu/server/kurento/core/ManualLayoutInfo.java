package io.openvidu.server.kurento.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import lombok.Getter;

import java.util.ArrayList;

public class ManualLayoutInfo {

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



    public ManualLayoutInfo(Session session) {
        this.session = session;
        this.roomId = session.getSessionId();
    }

    public void switchAutoMode() {
        session.getCompositeService().switchAutoMode(false);
        timestamp = 0;
        layoutModeType = LayoutModeTypeEnum.NORMAL;
        LayoutModeEnum mode = LayoutModeEnum.ONE;
    }

    public void updateLayout(long timestamp, LayoutModeEnum mode, LayoutModeTypeEnum layoutModeType,
                             JsonArray layoutJsonArray, Participant moderatorPart) {
        session.getCompositeService().switchAutoMode(false);
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

            layout.add(new Item(uuid, StreamType.valueOf(streamType)));
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

    static class Layout extends ArrayList<Item> {
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
