package org.gary.netframe.test.server;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.eventhandler.Reply;

public class ServerEventHandler extends EventHandler {

    @Override
    public Reply onRead(byte[] readBytes){
        System.out.println("server receive: "+new String(readBytes));
        return new Reply(true,"I am server, I got that".getBytes());
    }

}
