package org.gary.netframe.test.client;

import org.gary.netframe.eventhandler.ClientEventHandler;
import org.gary.netframe.nio.NioClient;

public class Client2 {

    public static void main(String[] args){
        ClientEventHandler eventHandler=new MyClientEventHandler();
        new NioClient(eventHandler).startup("127.0.0.1",8888);
        for (int i = 0; i < 100; i++) {
            eventHandler.writeToServer("hell nico from client 2 , id "+i);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
