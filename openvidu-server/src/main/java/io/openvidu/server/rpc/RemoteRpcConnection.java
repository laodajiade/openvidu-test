package io.openvidu.server.rpc;

import io.openvidu.server.client.RtcUserClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class RemoteRpcConnection implements IRpcConnection {

    @Autowired
    RtcUserClient rtcUserClient;

    @Override
    public RpcConnection putIfAbsent(String privateId, RpcConnection rpcConnection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RpcConnection get(String privateId) {
        return rtcUserClient.getRpcConnection(privateId);
    }

    @Override
    public List<RpcConnection> gets(Collection<String> privateIds) {
        return rtcUserClient.getRpcConnections(privateIds);
    }

    @Override
    public RpcConnection remove(String privateId) {
        return rtcUserClient.remove(privateId);
    }

    @Override
    public int size() {
        return rtcUserClient.size();
    }

    @Override
    public List<RpcConnection> getByUuids(Collection<String> uuids) {
        return rtcUserClient.getByUuids(uuids);
    }

    @Override
    public Collection<RpcConnection> values() {
        return rtcUserClient.values();
    }
}
