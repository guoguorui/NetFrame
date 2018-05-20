package org.gary.netframe.eventhandler;

import org.gary.netframe.common.BytesInt;
import org.gary.netframe.common.BytesObject;
import org.gary.netframe.nio.NioServer;

public class ServerEventHandler extends EventHandler {

    private NioServer nioServer;

    public void onActive(NioServer nioServer){
        this.nioServer=nioServer;
    }

    public boolean writeToAll(byte[] contents){
        return nioServer!=null && nioServer.writeToAll(contents);
    }

    public boolean writeToAll(String s){
        return writeToAll(s.getBytes());
    }

    public boolean writeToAll(int i){
        return writeToAll(BytesInt.int2bytes(i));
    }

    public boolean writeToAll(Object object, Class clazz){
        return writeToAll(BytesObject.serialize(object,clazz));
    }

    @Override
    public void onException(Throwable cause) {
        cause.printStackTrace();
        nioServer=null;
    }

}
