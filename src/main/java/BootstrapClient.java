import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class BootstrapClient {

    public  ByteBuffer writeBuffer=ByteBuffer.allocate(1024);
    public  ByteBuffer readBuffer=ByteBuffer.allocate(1024);
    public EventHandler eventHandler;

    public BootstrapClient(EventHandler eventHandler){
        this.eventHandler=eventHandler;
    }

    public void startup(String hostname,int port){
        SocketChannel socketChannel=null;
        Selector selector=null;
        try {
            selector=Selector.open();
            socketChannel=SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(hostname,port));
            while(true){
                selector.select();
                Iterator<SelectionKey> selectionKeyIterator=selector.selectedKeys().iterator();
                while(selectionKeyIterator.hasNext()){
                    SelectionKey selectionKey=selectionKeyIterator.next();
                    handle(selectionKey);
                    selectionKeyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void handle(SelectionKey selectionKey) throws IOException{
        if(selectionKey.isConnectable()){
            handleConnect(selectionKey);
        }
        if(selectionKey.isWritable()){
            handleWrite(selectionKey);
        }
        if(selectionKey.isReadable()){
            handleRead(selectionKey);
        }
    }

    public void handleConnect(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        if(selectionKey.isConnectable()){
            socketChannel.finishConnect();
        }
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void handleWrite(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        writeBuffer.clear();
        byte[] writeBytes=eventHandler.sharedWriteQueue.poll();
        if(writeBytes==null)
            return;
        writeBuffer.put(writeBytes);
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
    }

    public void handleRead(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        readBuffer.clear();
        int readBytes=socketChannel.read(readBuffer);
        if(readBytes>0){
            //System.out.println("client receive: "+new String(readBuffer.array(),0,readBytes));
            String readString=new String(readBuffer.array(),0,readBytes);
            Reply reply=eventHandler.onRead(readString.getBytes());
            if(reply.isWriteBack()){
                eventHandler.sharedWriteQueue.offer(reply.getWriteBytes());
            }
        }
    }

}
