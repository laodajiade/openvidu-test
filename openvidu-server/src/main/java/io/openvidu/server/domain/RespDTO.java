package io.openvidu.server.domain;

import lombok.Data;

@Data
public class RespDTO {

    private static final String DEFAULT_JSON_RPC_VERSION = "2.0";


    private Integer id;

    private String jsonprc = DEFAULT_JSON_RPC_VERSION;

    private Object result;

    private String uuid;

    private Long corpId;

    private String project;

    private String participantPrivateId;

    public String getJsonprc() {
        return jsonprc = DEFAULT_JSON_RPC_VERSION;
    }

    public void setJsonprc(String jsonprc) {
        this.jsonprc = DEFAULT_JSON_RPC_VERSION;
    }
}
