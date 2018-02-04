import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

//处理字节超过1024的情况
//使读写模式自由可扩展
//线程模型

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
            handleWrite(selectionKey);
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

    public void handleWrite(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        writeBuffer.clear();
        byte[] writeByteArray=eventHandler.onWrite();
        //writeBuffer.put(eventHandler.onWrite());
        for(int i=0;i<writeByteArray.length;i=i+1024){
            for(int j=i;j<i+1024 && j<writeByteArray.length;j++){
                writeBuffer.put(writeByteArray[j]);
            }
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
        }
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);
    }

    public void handleRead(SelectionKey selectionKey) throws IOException{
        byte[] readByteArray=null;
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        readBuffer.clear();
        try {
            int readBytesCount=socketChannel.read(readBuffer);
            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
            while(readBytesCount>0){
                byteArrayOutputStream.write(readBuffer.array(),0,readBytesCount);
                readBuffer.clear();
                readBytesCount=socketChannel.read(readBuffer);
            }
            readByteArray=byteArrayOutputStream.toByteArray();
            eventHandler.onRead(readByteArray);
        }
        catch (IOException e){
            socketChannel.close();
            selectionKey.cancel();
        }
        //socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE);
    }

}
