public class DefaultEventHandler implements EventHandler {

    //用户接收从客户端读取到的字节数组
    public void onRead(byte[] readBytes){
        System.out.println("server has read: "+new String(readBytes));
    }

    //用户选择字节数组发送给客户端
    public byte[] onWrite(){
        String writeMessage="hello nico from server";
        return writeMessage.getBytes();
    }
}
