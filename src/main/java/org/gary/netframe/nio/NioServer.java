package org.gary.netframe.nio;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.eventhandler.Reply;

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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//具有自动调节threshold的能力
//连接中断时资源回收
//给客户端添加写数据的接口
//解决TCP粘包问题

public class NioServer {

    private Queue<ByteBuffer> writeBufferQueue;
    private Queue<ByteBuffer> readBufferQueue;
    private EventHandler eventHandler;
    private ThreadPoolExecutor threadPoolExecutor;
    private HashMap<SelectionKey,Queue<byte[]>> keyToWriteByteArrayQueue =new HashMap<SelectionKey,Queue<byte[]>>();

    public NioServer(EventHandler eventHandler, int threshold){
        this.eventHandler=eventHandler;
        this.threadPoolExecutor=new ThreadPoolExecutor(threshold,50,60, TimeUnit.SECONDS,new LinkedBlockingQueue<>());
        this.writeBufferQueue=new LinkedBlockingQueue<>(threshold);
        this.readBufferQueue=new LinkedBlockingQueue<>(threshold);
        for(int i=0;i<threshold;i++){
            writeBufferQueue.add(ByteBuffer.allocate(1024));
            readBufferQueue.add(ByteBuffer.allocate(1024));
        }
    }

    public NioServer startup(final int port){
        new Thread(()->{
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
                    if(selector.selectedKeys().size()>1){
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
        }).start();
        return this;
    }

    private void handle(SelectionKey selectionKey) throws IOException{
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

    private void handleAccept(SelectionKey selectionKey) throws IOException{
        ServerSocketChannel serverSocketChannel=(ServerSocketChannel)selectionKey.channel();
        SocketChannel socketChannel=(SocketChannel)serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        SelectionKey selectionKey1=socketChannel.register(selectionKey.selector(),SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        keyToWriteByteArrayQueue.put(selectionKey1,new LinkedBlockingQueue<byte[]>());
    }


    private void handleWrite(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        byte[] writeByteArray= keyToWriteByteArrayQueue.get(selectionKey).poll();
        if(writeByteArray==null){
            return;
        }
        //取太早缓存区会疏漏回收
        ByteBuffer writeBuffer=writeBufferQueue.poll();
        if(writeBuffer==null){
            return;
        }
        writeBuffer.clear();
        for(int i=0;i<writeByteArray.length;i=i+1024){
            int length=Math.min(1024,writeByteArray.length-i);
            writeBuffer.put(writeByteArray,i,length);
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
            writeBuffer.clear();
        }
        writeBufferQueue.offer(writeBuffer);
    }

    private void handleRead(SelectionKey selectionKey) throws IOException{
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
                Queue<byte[]> queue= keyToWriteByteArrayQueue.get(selectionKey);
                queue.offer(reply.getWriteBytes());
            }
        }
        catch (IOException e){
            socketChannel.close();
            selectionKey.cancel();
        }
        readBufferQueue.add(readBuffer);
    }

    public void writeToAll(byte[] writeBytes){
        for(Map.Entry<SelectionKey,Queue<byte[]>> entry: keyToWriteByteArrayQueue.entrySet()){
            entry.getValue().offer(writeBytes);
        }
    }

}
