package org.gary.netframe.eventhandler;

import org.gary.netframe.common.BytesInt;
import org.gary.netframe.nio.NioClient;

public class ClientEventHandler extends EventHandler{

    private NioClient nioClient;

    public void onActive(NioClient nioClient){
        this.nioClient=nioClient;
    }

    public void writeToServer(byte[] content){
        nioClient.writeToServer(content);
    }

    public void writeToServer(String s){
         writeToServer(s.getBytes());
    }

    public void writeToServer(int i){
         writeToServer(BytesInt.int2bytes(i));
    }

    @Override
    public void onException(Throwable cause) {
        nioClient=null;
    }

    public void connectAvailable() throws Exception{
        nioClient.connectAvailable();
    }


}
