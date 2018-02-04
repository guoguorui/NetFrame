import java.util.LinkedList;
import java.util.Queue;

public abstract class EventHandler {

    public Queue<byte[]> queue=new LinkedList<byte[]>();

    public abstract void onRead(byte[] readBytes);

    public void write(byte[] writeBytes){
        queue.offer(writeBytes);
    }
}
