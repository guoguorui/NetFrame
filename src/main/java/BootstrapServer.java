import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//具有自动调节threshold的能力
//考虑数据结构选择LinkedBlockingQueue是否合适
//优化线程模型
//client支持超过1024字节的传输
//当客户端断开后保持服务器正常运行

public class BootstrapServer {

    private Queue<ByteBuffer> writeBufferQueue;
    private Queue<ByteBuffer> readBufferQueue;
    private EventHandler eventHandler;
    private ThreadPoolExecutor threadPoolExecutor;
    private int threshold;
    private HashMap<SelectionKey,Queue<byte[]>> keyToWriteQueue=new HashMap<SelectionKey,Queue<byte[]>>();

    public BootstrapServer(EventHandler eventHandler, int threshold){
        this.eventHandler=eventHandler;
        this.threshold=threshold;
        this.threadPoolExecutor=new ThreadPoolExecutor(threshold,50,60, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
        this.writeBufferQueue=new LinkedBlockingQueue<>(threshold);
        this.readBufferQueue=new LinkedBlockingQueue<>(threshold);
        for(int i=0;i<threshold;i++){
            writeBufferQueue.add(ByteBuffer.allocate(1024));
            readBufferQueue.add(ByteBuffer.allocate(1024));
        }
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
        SelectionKey selectionKey1=socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        keyToWriteQueue.put(selectionKey1,new LinkedBlockingQueue<byte[]>());
    }


    public void handleWrite(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        ByteBuffer writeBuffer=writeBufferQueue.poll();
        if(writeBuffer==null)
            return;
        writeBuffer.clear();
        byte[] writeByteArray=keyToWriteQueue.get(selectionKey).poll();
        if(writeByteArray==null){
            writeByteArray=eventHandler.sharedWriteQueue.poll();
            if(writeByteArray==null)
                return;
        }
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
        writeBufferQueue.add(writeBuffer);
        //socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);
    }

    public void handleRead(SelectionKey selectionKey) throws IOException{
        byte[] readByteArray=null;
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        ByteBuffer readBuffer=readBufferQueue.poll();
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
            Reply reply=eventHandler.onRead(readByteArray);
            if(reply.isWriteBack()) {
                Queue<byte[]> queue=keyToWriteQueue.get(selectionKey);
                queue.offer(reply.getWriteBytes());
            }
        }
        catch (IOException e){
            socketChannel.close();
            selectionKey.cancel();
        }
        readBufferQueue.add(readBuffer);
        //socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE);
    }

}
