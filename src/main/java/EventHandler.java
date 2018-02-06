import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class EventHandler {

    public Queue<byte[]> writeQueue =new LinkedBlockingQueue<>();

    public abstract Reply onRead(byte[] readBytes);

}
