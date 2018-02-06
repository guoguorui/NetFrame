public class ServerEventHandler extends EventHandler{

    @Override
    public Reply onRead(byte[] readBytes){
        System.out.println("server receive: "+new String(readBytes));
        return new Reply(false,null);
    }

}
