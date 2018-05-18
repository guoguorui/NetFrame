package org.gary.netframe.test.client;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.nio.NioClient;

public class Client {

    public static void main(String[] args){
        EventHandler eventHandler=new ClientEventHandler();
        NioClient nioClient=new NioClient(eventHandler).startup("127.0.0.1",8888);
        nioClient.writeToServer("hell nico from client".getBytes());
    }
}
