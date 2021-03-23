package io.openvidu.server.rpc;

import java.util.Collection;

public interface IRpcConnection {

    RpcConnection putIfAbsent(String privateId, RpcConnection rpcConnection);

    RpcConnection get(String privateId);

    RpcConnection remove(String privateId);

    int size();

    Collection<RpcConnection> values();
}
