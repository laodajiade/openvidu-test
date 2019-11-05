package io.openvidu.server.common.manage;

import io.openvidu.server.rpc.RpcConnection;

/**
 * @author chosongi
 * @date 2019/10/16 17:11
 */
public interface AuthorizationManage {
    /**
     * check if user's operations permitted(exclude 'accessIn';'setAudioStatus';'setVideoStatus';'setAudioSpeakerStatus')
     * @param method
     * @param rpcConnection
     * @return
     */
    boolean checkIfOperationPermitted(String method, RpcConnection rpcConnection);
}
