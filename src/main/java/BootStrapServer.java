import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


//处理线程安全

public class BootStrapServer {

    private ByteBuffer writeBuffer=ByteBuffer.allocate(1024);
    private ByteBuffer readBuffer=ByteBuffer.allocate(1024);
    private EventHandler eventHandler;
    private ThreadPoolExecutor threadPoolExecutor;
    private int threshold;



    public BootStrapServer(EventHandler eventHandler,int threshold){
        this.eventHandler=eventHandler;
        this.threadPoolExecutor=new ThreadPoolExecutor(threshold,50,60, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    public void startup(final int port){
        new Thread(){
            @Override
            public void run() {
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
                        if(selector.selectedKeys().size()>threshold){
                            while(selectionKeyIterator.hasNext()){
                                SelectionKey selectionKey=selectionKeyIterator.next();
                                threadPoolExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            handle(selectionKey);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                selectionKeyIterator.remove();
                            }
                        }else {
                            while(selectionKeyIterator.hasNext()){
                                SelectionKey selectionKey=selectionKeyIterator.next();
                                handle(selectionKey);
                                selectionKeyIterator.remove();
                            }
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
        }.start();


    }

    public void handle(SelectionKey selectionKey) throws IOException{
        if(selectionKey.isAcceptable()){
            handleAccept(selectionKey);
        }
        if(selectionKey.isWritable()){
            handleWrite(selectionKey);
        }
        if(selectionKey.isReadable()){
            handleRead(selectionKey);
        }
    }

    public void handleAccept(SelectionKey selectionKey) throws IOException{
        ServerSocketChannel serverSocketChannel=(ServerSocketChannel)selectionKey.channel();
        SocketChannel socketChannel=(SocketChannel)serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }


    public void handleWrite(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        writeBuffer.clear();
        //byte[] writeByteArray=eventHandler.write();
        byte[] writeByteArray=eventHandler.queue.poll();
        if(writeByteArray==null)
            return;
        if(writeByteArray.length<1024){
            writeBuffer.put(writeByteArray);
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
        }
        else{
            for(int i=0;i<writeByteArray.length;i=i+1024){
                for(int j=i;j<i+1024 && j<writeByteArray.length;j++){
                    writeBuffer.put(writeByteArray[j]);
                }
                writeBuffer.flip();
                socketChannel.write(writeBuffer);
                writeBuffer.clear();
            }
        }
        //socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);
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
