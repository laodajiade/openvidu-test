package io.openvidu.server.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestDTO extends BasicDTO {

    private Integer id;

    private String jsonprc;

    private String method;

    private String params;

    private String uuid;

    private String participantPrivateId;

    private String origin;

    /**
     * 这个用户现在注册在哪个信令服务
     */
    private String openviduId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getJsonprc() {
        return jsonprc;
    }

    public void setJsonprc(String jsonprc) {
        this.jsonprc = jsonprc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getParticipantPrivateId() {
        return participantPrivateId;
    }

    public void setParticipantPrivateId(String participantPrivateId) {
        this.participantPrivateId = participantPrivateId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
