package org.gary.netframe.test.server;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.nio.NioServer;

public class Server {

    public static void main(String[] args)
    {
        EventHandler eventHandler=new ServerEventHandler();
        NioServer nioServer=new NioServer(eventHandler,10).startup(8888);
        for(int i=0;i<100;i++){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nioServer.writeToAll(("hello nico per second "+i).getBytes());
            System.out.println("server queue: "+i);
        }
    }

}
