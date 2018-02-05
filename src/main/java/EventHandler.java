import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class EventHandler {

    public Queue<byte[]> sharedWriteQueue =new LinkedBlockingQueue<>();

    public abstract Reply onRead(byte[] readBytes);

    public void writeToAll(byte[] writeBytes){
        sharedWriteQueue.offer(writeBytes);
    }
}
