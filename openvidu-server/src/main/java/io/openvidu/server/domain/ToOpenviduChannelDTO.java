package io.openvidu.server.domain;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

public class ToOpenviduChannelDTO {


    private String method;

    @Setter
    @Getter
    private JsonObject params = new JsonObject();

    public ToOpenviduChannelDTO(String method) {
        this.method = method;
    }

    public ToOpenviduChannelDTO addProperty(String property, String value) {
        params.addProperty(property, value);
        return this;
    }


    public String toJsonStr() {
        JsonObject msg = new JsonObject();
        msg.addProperty("method", method);
        msg.add("params", params);
        return msg.toString();
    }

    @Override
    public String toString() {
        return toJsonStr();
    }
}
