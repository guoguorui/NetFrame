package org.gary.netframe.test.server;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.eventhandler.Reply;

public class ServerEventHandler extends EventHandler {

    @Override
    public Reply onRead(byte[] readBytes){
        String receive = new String(readBytes);
        System.out.println("server receive: " + receive);
        String[] ss = receive.split(" ");
        String id =ss[ss.length-1];
        return new Reply(true,("I am server, I got that "+id).getBytes());
    }

}
