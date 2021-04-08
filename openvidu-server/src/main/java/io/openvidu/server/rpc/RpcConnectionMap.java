package io.openvidu.server.rpc;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RpcConnectionMap implements IRpcConnection {

    private final ConcurrentMap<String, RpcConnection> rpcConnections = new ConcurrentHashMap<>();


    @Override
    public RpcConnection putIfAbsent(String privateId, RpcConnection rpcConnection) {
        return rpcConnections.putIfAbsent(privateId, rpcConnection);
    }

    @Override
    public RpcConnection get(String privateId) {
        return rpcConnections.get(privateId);
    }

    @Override
    public RpcConnection remove(String privateId) {
        return rpcConnections.remove(privateId);
    }

    @Override
    public int size() {
        return rpcConnections.size();
    }

    @Override
    public Collection<RpcConnection> values() {
        return rpcConnections.values();
    }
}
