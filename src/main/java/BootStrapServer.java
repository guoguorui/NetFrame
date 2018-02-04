import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class BootStrapServer {

    public  ByteBuffer writeBuffer=ByteBuffer.allocate(1024);
    public  ByteBuffer readBuffer=ByteBuffer.allocate(1024);
    public EventHandler eventHandler;

    public BootStrapServer(EventHandler eventHandler){
        this.eventHandler=eventHandler;
    }

    public void startup(int port){
        ServerSocketChannel serverSocketChannel=null;
        Selector selector=null;
        try {
            selector=Selector.open();
            serverSocketChannel=ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
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
        } finally {
            if(selector!=null){
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(serverSocketChannel!=null){
                try {
                    serverSocketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void handle(SelectionKey selectionKey) throws IOException{
        if(selectionKey.isAcceptable()){
            handleAccept(selectionKey);
        }
        else if(selectionKey.isWritable()){
            handleWrite(selectionKey,"server");
        }
        else if(selectionKey.isReadable()){
            handleRead(selectionKey);
        }
    }

    public void handleAccept(SelectionKey selectionKey) throws IOException{
        ServerSocketChannel serverSocketChannel=(ServerSocketChannel)selectionKey.channel();
        SocketChannel socketChannel=(SocketChannel)serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE);
    }

    public void handleWrite(SelectionKey selectionKey,String from) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        writeBuffer.clear();
        writeBuffer.put(eventHandler.onWrite(from).getBytes());
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);
    }

    public void handleRead(SelectionKey selectionKey) throws IOException{
        String message=null;
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        readBuffer.clear();
        try {
            int readBytes = socketChannel.read(readBuffer);
            if(readBytes>=0){
                message=new String(readBuffer.array(),0,readBytes);
                //System.out.println("server receive: "+message);
            }
            eventHandler.onRead(message);
        }
        catch (IOException e){
            socketChannel.close();
            selectionKey.cancel();
        }

        //socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE);
    }

}
