package org.gary.netframe.eventhandler;

import org.gary.netframe.nio.NioClient;
import org.gary.netframe.nio.NioServer;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class EventHandler {

    public Reply onRead(byte[] readBytes){
        return new Reply(false,null);
    };

    public abstract void onException(Throwable cause);

}
