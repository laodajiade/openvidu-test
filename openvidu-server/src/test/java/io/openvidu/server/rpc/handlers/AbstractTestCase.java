package io.openvidu.server.rpc.handlers;

import io.openvidu.server.rpc.RpcConnection;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.internal.client.ClientSession;

public abstract class AbstractTestCase {


    public RpcConnection getEmptyRc() {
        Session session = new ClientSession(RandomStringUtils.randomAlphanumeric(6), new Object());
        return new RpcConnection(session);
    }
}
