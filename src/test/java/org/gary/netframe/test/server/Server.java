package org.gary.netframe.test.server;

import org.gary.netframe.eventhandler.ServerEventHandler;
import org.gary.netframe.nio.NioServer;

public class Server {

    public static void main(String[] args)
    {
        ServerEventHandler eventHandler=new MyServerEventHandler();
        new NioServer(eventHandler).startup(8888);
        /*for (int i = 0; i < 100; i++) {
            if(!eventHandler.writeToAll(("hello nico from server "+i).getBytes()))
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
    }

}
