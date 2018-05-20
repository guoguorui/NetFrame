package org.gary.netframe.eventhandler;

import org.gary.netframe.common.BytesInt;
import org.gary.netframe.common.BytesObject;
import org.gary.netframe.nio.NioClient;

public abstract class ClientEventHandler extends EventHandler{

    private NioClient nioClient;

    public void onActive(NioClient nioClient){
        this.nioClient=nioClient;
    }

    public boolean writeToServer(byte[] content){
        return nioClient!=null && nioClient.writeToServer(content);
    }

    public boolean writeToServer(String s){
        return writeToServer(s.getBytes());
    }

    public boolean writeToServer(int i){
        return writeToServer(BytesInt.int2bytes(i));
    }

    public boolean writeToServer(Object object, Class clazz){
        return writeToServer(BytesObject.serialize(object,clazz));
    }

    @Override
    public void onException(Throwable cause) {
        cause.printStackTrace();
        nioClient=null;
    }


}
