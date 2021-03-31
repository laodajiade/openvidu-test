package io.openvidu.server.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class SendMsgVO {

    private String clientMsgId;

    private String ruid;

    private String roomId;

    private Long timestamp;
    /**
     * 消息类型：
     * 0: 表示文本消息，
     * 1: 表示图片，
     * 2: 表示语音，
     * 3: 表示视频，
     * 4: 表示文件
     */
    private Integer msgType;
    /**
     * 重发消息标记，0：非重发消息，1：重发消息，如重发消息会按照clientMsgId检查去重逻辑
     */
    private Integer resendFlag;

    /**
     * 0：点对点个人消息，1：群消息
     */
    private Integer operate;

    /**
     * 特定的接收者者，operate=0时必选
     */
    private List<String> reciverAccount;
    /**
     * 被at对象
     */
    private List<String> atAccount;

    private String senderAccount;

    /**
     * 消息内容  长度1000
     */
    private String content;

    /**
     * 消息扩展字段，使用json格式，长度限制4096个字符
     */
    private String ext;
}
