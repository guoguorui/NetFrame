package org.gary.netframe.test.client;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.eventhandler.Reply;

public class ClientEventHandler extends EventHandler {

    @Override
    public Reply onRead(byte[] readBytes) {
        System.out.println("client receive: "+new String(readBytes));
        return new Reply(true,"hello nico from client".getBytes());
    }
}
