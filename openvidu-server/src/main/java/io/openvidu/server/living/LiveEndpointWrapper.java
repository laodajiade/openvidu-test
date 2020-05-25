package io.openvidu.server.living;

import com.google.gson.JsonObject;
import org.kurento.client.LiveEndpoint;

public class LiveEndpointWrapper {

    private LiveEndpoint liver;
    private String connectionId;
    private String livingId;
    private String streamId;
    private String clientData;
    private String serverData;
    private boolean hasAudio;
    private boolean hasVideo;
    private String typeOfVideo;

    private long startTime;
    private long endTime;
    private long size;

    public LiveEndpointWrapper(LiveEndpoint liver, String connectionId, String livingId, String streamId,
                               String clientData, String serverData, boolean hasAudio, boolean hasVideo, String typeOfVideo) {
        this.liver = liver;
        this.connectionId = connectionId;
        this.livingId = livingId;
        this.streamId = streamId;
        this.clientData = clientData;
        this.serverData = serverData;
        this.hasAudio = hasAudio;
        this.hasVideo = hasVideo;
        this.typeOfVideo = typeOfVideo;
    }

    public LiveEndpoint getLiver() {
        return liver;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getLivingId() {
        return livingId;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getClientData() {
        return clientData;
    }

    public String getServerData() {
        return serverData;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean hasAudio() {
        return hasAudio;
    }

    public boolean hasVideo() {
        return hasVideo;
    }

    public String getTypeOfVideo() {
        return typeOfVideo;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("connectionId", this.connectionId);
        json.addProperty("streamId", this.streamId);
        json.addProperty("clientData", this.clientData);
        json.addProperty("serverData", this.serverData);
        json.addProperty("startTime", this.startTime);
        json.addProperty("endTime", this.endTime);
        json.addProperty("duration", this.endTime - this.startTime);
        json.addProperty("size", this.size);
        json.addProperty("hasAudio", this.hasAudio);
        json.addProperty("hasVideo", this.hasVideo);
        if (this.hasVideo) {
            json.addProperty("typeOfVideo", this.typeOfVideo);
        }
        return json;
    }

}
