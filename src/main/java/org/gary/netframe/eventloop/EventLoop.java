package org.gary.netframe.eventloop;

import org.gary.netframe.buffer.Pending;
import org.gary.netframe.buffer.StickyBuffer;
import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.eventhandler.Reply;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//多个Channel共享一个EventLoop
class EventLoop implements Runnable {

    private HashMap<SelectionKey, Pending> map = new HashMap<>();
    private ByteBuffer headBuffer = ByteBuffer.allocate(4);
    private SelectionKey selectionKey;
    private EventHandler eventHandler;
    BlockingQueue<EventSource> queue = new LinkedBlockingQueue<>();

    EventLoop(){
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        while (getSource()){
            try {
                //System.out.println(Thread.currentThread()+"开始处理事件");
                handle(selectionKey, eventHandler);
            } catch (IOException e) {
                SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                Socket socket = socketChannel.socket();
                System.out.println("客户端主动中断" + socket.getInetAddress() + ":" + socket.getPort());
                map.remove(selectionKey);
                selectionKey.cancel();
            }
        }
    }

    private boolean getSource(){
        try {
            EventSource source = queue.take();
            setSource(source.getSelectionKey(),source.getEventHandler());
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void setSource(SelectionKey selectionKey, EventHandler eventHandler) {
        this.selectionKey = selectionKey;
        this.eventHandler = eventHandler;
    }

    private void handle(SelectionKey selectionKey, EventHandler eventHandler) throws IOException {
        if(!selectionKey.isValid())
            return;
        if (map.get(selectionKey) == null)
            map.put(selectionKey, new Pending());
        if (selectionKey.isWritable()) {
            handleWrite(selectionKey);
        }
        if (selectionKey.isReadable()) {
            handleRead(selectionKey, eventHandler);
        }
    }

    private void handleWrite(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Pending pending = map.get(selectionKey);
        byte[] writeByteArray = pending.getWriteQueue().poll();
        if (writeByteArray == null) {
            return;
        }
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
        int contentLength = writeByteArray.length;
        writeBuffer.putInt(contentLength);
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        writeBuffer.clear();
        for (int i = 0; i < writeByteArray.length; i = i + 1024) {
            int length = Math.min(1024, writeByteArray.length - i);
            writeBuffer.put(writeByteArray, i, length);
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
            writeBuffer.clear();
        }
    }

    private void handleRead(SelectionKey selectionKey, EventHandler eventHandler) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Pending pending = map.get(selectionKey);
        StickyBuffer stickyBuffer = pending.getStickyBuffer();
        if (stickyBuffer == null)
            stickyBuffer = new StickyBuffer();
        int contentLength = stickyBuffer.getContentLength();
        ByteBuffer readBuffer = stickyBuffer.getByteBuffer();
        if (contentLength == -1) {
            socketChannel.read(headBuffer);
            if (!headBuffer.hasRemaining()) {
                headBuffer.flip();
                contentLength = headBuffer.getInt();
                stickyBuffer.setContentLength(contentLength);
                stickyBuffer.setByteBuffer(ByteBuffer.allocate(contentLength));
                headBuffer.clear();
            }
        } else {
            socketChannel.read(readBuffer);
            if (!readBuffer.hasRemaining()) {
                Reply reply = eventHandler.onRead(readBuffer.array());
                if (reply.isWriteBack()) {
                    Queue<byte[]> queue = pending.getWriteQueue();
                    queue.offer(reply.getWriteBytes());
                }
                stickyBuffer.setByteBuffer(null);
                stickyBuffer.setContentLength(-1);
            }
        }
        //此时的stickBuffer是被新建的，需要保存
        pending.setStickyBuffer(stickyBuffer);
    }

}
