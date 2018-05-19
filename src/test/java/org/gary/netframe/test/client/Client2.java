package org.gary.netframe.test.client;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.nio.NioClient;

public class Client2 {

    public static void main(String[] args){
        EventHandler eventHandler=new ClientEventHandler();
        NioClient nioClient=new NioClient(eventHandler).startup("127.0.0.1",8888);
        for (int i = 0; i < 100; i++) {
            try {
                nioClient.writeToServer("hell nico from client 2 , id "+i);
            } catch (Exception e) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
