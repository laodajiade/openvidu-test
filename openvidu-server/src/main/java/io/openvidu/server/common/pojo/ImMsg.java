package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class ImMsg implements Serializable {
    private Long id;

    /**
     * 客户端本地的id，去重时使用
     */
    private String clientMsgId;

    private String ruid;

    private String roomId;

    /**
     * 时间戳
     */
    private Date timestamp;

    /**
     * 消息类型：0: 表示文本消息，1: 表示图片，2: 表示语音，3: 表示视频，4: 表示文件
     */
    private Integer msgType;

    /**
     * 0：点对点个人消息，1：群消息
     */
    private Integer operate;

    private String atAccount;

    /**
     * 发送者的userid
     */
    private Long senderUserId;

    /**
     * 接收者的userid
     */
    private Long revicerUserId;

    /**
     * 内容
     */
    private String content;

    /**
     * 扩展字段
     */
    private String ext;

    private static final long serialVersionUID = 1L;
}