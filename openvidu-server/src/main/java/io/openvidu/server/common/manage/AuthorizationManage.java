package io.openvidu.server.common.manage;

import io.openvidu.server.rpc.RpcConnection;

/**
 * @author chosongi
 * @date 2019/10/16 17:11
 */
public interface AuthorizationManage {
    boolean checkIfOperationPermitted(String method, RpcConnection rpcConnection);
}
