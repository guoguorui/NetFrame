package org.gary.netframe.nio;

import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.eventhandler.Reply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class NioClient {

    private EventHandler eventHandler;
    private Queue<byte[]> writeQueue =new LinkedBlockingQueue<>();
    private ByteBuffer writeBuffer=ByteBuffer.allocate(1024);
    private ByteBuffer readBuffer;
    private ByteBuffer headBuffer=ByteBuffer.allocate(4);
    private int contentLength=-1;

    public NioClient(EventHandler eventHandler){
        this.eventHandler=eventHandler;
    }

    public NioClient startup(String hostname,int port){
        new Thread(()->{SocketChannel socketChannel=null;
            Selector selector=null;
            try {
                selector=Selector.open();
                socketChannel=SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
                socketChannel.connect(new InetSocketAddress(hostname,port));
                while (!Thread.interrupted()) {
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
        }).start();
        return this;
    }

    private void handle(SelectionKey selectionKey) throws IOException{
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

    private void handleConnect(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        if(selectionKey.isConnectable()){
            socketChannel.finishConnect();
        }
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private void handleWrite(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        writeBuffer.clear();
        byte[] writeByteArray=writeQueue.poll();
        if(writeByteArray==null)
            return;
        int contentLength=writeByteArray.length;
        writeBuffer.putInt(contentLength);
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        writeBuffer.clear();
        for(int i=0;i<writeByteArray.length;i=i+1024){
            int length=Math.min(1024,writeByteArray.length-i);
            writeBuffer.put(writeByteArray,i,length);
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
            writeBuffer.clear();
        }
    }

    private void handleRead(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        if(contentLength==-1){
            socketChannel.read(headBuffer);
            if(!headBuffer.hasRemaining()){
                headBuffer.flip();
                contentLength=headBuffer.getInt();
                readBuffer=ByteBuffer.allocate(contentLength);
                headBuffer.clear();
            }
        }else{
            socketChannel.read(readBuffer);
            if(!readBuffer.hasRemaining()){
                Reply reply=eventHandler.onRead(readBuffer.array());
                if(reply.isWriteBack()) {
                    writeQueue.offer(reply.getWriteBytes());
                }
                contentLength=-1;
            }
        }
    }

    public void writeToServer(byte[] writeBytes){
        writeQueue.offer(writeBytes);
    }

}
