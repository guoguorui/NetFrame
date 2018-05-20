package org.gary.netframe.test.client;

import org.gary.netframe.eventhandler.ClientEventHandler;
import org.gary.netframe.eventhandler.Reply;

public class MyClientEventHandler extends ClientEventHandler {

    @Override
    public Reply onRead(byte[] readBytes) {
        System.out.println("client receive: "+new String(readBytes));
        //return new Reply(true,"hello nico from client".getBytes());
        return new Reply(false,null);
    }

}
