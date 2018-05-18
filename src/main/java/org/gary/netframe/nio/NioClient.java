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

public class NioClient {

    private ByteBuffer writeBuffer=ByteBuffer.allocate(1024);
    private ByteBuffer readBuffer=ByteBuffer.allocate(1024);
    private EventHandler eventHandler;

    public NioClient(EventHandler eventHandler){
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
        byte[] writeByteArray=eventHandler.writeQueue.poll();
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
    }

    public void handleRead(SelectionKey selectionKey) throws IOException{
        SocketChannel socketChannel=(SocketChannel) selectionKey.channel();
        readBuffer.clear();
        byte[] readByteArray=null;
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
            eventHandler.writeQueue.offer(reply.getWriteBytes());
        }

    }

}
