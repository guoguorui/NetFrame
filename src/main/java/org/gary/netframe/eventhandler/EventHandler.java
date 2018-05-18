package org.gary.netframe.eventhandler;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class EventHandler {

    public abstract Reply onRead(byte[] readBytes);

}
