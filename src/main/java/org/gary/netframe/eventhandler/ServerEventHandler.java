package org.gary.netframe.eventhandler;

import org.gary.netframe.common.BytesInt;
import org.gary.netframe.nio.NioServer;

public class ServerEventHandler extends EventHandler {

    private NioServer nioServer;

    public void onActive(NioServer nioServer) {
        this.nioServer = nioServer;
    }

    public void writeToAll(byte[] contents) {
        nioServer.writeToAll(contents);
    }

    public void writeToAll(String s) {
        writeToAll(s.getBytes());
    }

    public void writeToAll(int i) {
        writeToAll(BytesInt.int2bytes(i));
    }

    @Override
    public void onException(Throwable cause) {
        nioServer = null;
    }

    public void connectAvailable() throws Exception{
        nioServer.connectAvailable();
    }

}
