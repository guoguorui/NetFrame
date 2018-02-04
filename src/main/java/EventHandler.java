public interface EventHandler {

    public void onRead(byte[] readBytes);

    public byte[] onWrite();

}
