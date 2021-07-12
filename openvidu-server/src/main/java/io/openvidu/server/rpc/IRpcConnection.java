package io.openvidu.server.rpc;

import java.util.Collection;
import java.util.List;

public interface IRpcConnection {

    RpcConnection putIfAbsent(String privateId, RpcConnection rpcConnection);

    RpcConnection get(String privateId);

    List<RpcConnection> gets(Collection<String> privateId);

    RpcConnection remove(String privateId);

    int size();

    Collection<RpcConnection> values();
}
