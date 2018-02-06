public class ClientEventHandler extends EventHandler {

    @Override
    public Reply onRead(byte[] readBytes) {
        System.out.println("client receive: "+new String(readBytes));
        return new Reply(true,"hello nico from client".getBytes());
    }
}
