import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioClient {

    public static ByteBuffer writeBuffer=ByteBuffer.allocate(1024);
    public static ByteBuffer readBuffer=ByteBuffer.allocate(1024);

    public static void main(String[] args){
        startup();
    }

    public static void startup(){
        SocketChannel socketChannel=null;
        Selector selector=null;
        try {
            selector=Selector.open();
            socketChannel=SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress("127.0.0.1",8080));
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

    public static void handle(SelectionKey selectionKey) throws IOException{
        if(selectionKey.isConnectable()){
            handleConnect(selectionKey);
        }
        else if(selectionKey.isWritable()){
            handleWrite(selectionKey);
        }
        else if(selectionKey.isReadable()){
            handleRead(selectionKey);
        }
    }

    public static void handleConnect(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        if(selectionKey.isConnectable()){
            socketChannel.finishConnect();
        }
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);
    }

    public static void handleWrite(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        writeBuffer.clear();
        writeBuffer.put("hello nico from client".getBytes());
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        //socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);
    }

    public static void handleRead(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        readBuffer.clear();
        int readBytes=socketChannel.read(readBuffer);
        if(readBytes>0){
            System.out.println("client receive: "+new String(readBuffer.array(),0,readBytes));
        }
        //socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE);
    }

}