package org.gary.netframe.nio;

import org.gary.netframe.buffer.StickyBuffer;
import org.gary.netframe.eventhandler.EventHandler;
import org.gary.netframe.eventhandler.Reply;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.Buffer;
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

//具有自动调节threshold的能力
//连接中断时资源回收
//处理并发安全
//考虑心跳检测

public class NioServer {

    private EventHandler eventHandler;
    private HashMap<SelectionKey, Queue<byte[]>> keyToWriteByteArrayQueue = new HashMap<>();
    private HashMap<SelectionKey, StickyBuffer> keyToReadBuffer = new HashMap<>();
    private ByteBuffer headBuffer = ByteBuffer.allocate(4);

    public NioServer(EventHandler eventHandler, int threshold) {
        this.eventHandler = eventHandler;
    }

    public NioServer startup(final int port) {
        new Thread(() -> {
            ServerSocketChannel serverSocketChannel = null;
            Selector selector = null;
            try {
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(port));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                while (!Thread.interrupted()) {
                    selector.select();
                    Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
                    while (selectionKeyIterator.hasNext()) {
                        SelectionKey selectionKey = selectionKeyIterator.next();
                        try {
                            handle(selectionKey);
                        } catch (IOException e) {
                            SocketChannel socketChannel=(SocketChannel)selectionKey.channel();
                            Socket socket=socketChannel.socket();
                            System.out.println("客户端主动中断"+socket.getInetAddress()+":"+socket.getPort());
                            selectionKey.cancel();
                        }finally {
                            selectionKeyIterator.remove();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (selector != null) {
                    try {
                        selector.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (serverSocketChannel != null) {
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

    private void handle(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isAcceptable()) {
            handleAccept(selectionKey);
        }
        if (selectionKey.isWritable()) {
            handleWrite(selectionKey);
        }
        if (selectionKey.isReadable()) {
            handleRead(selectionKey);
        }
    }

    private void handleAccept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        SelectionKey selectionKey1 = socketChannel.register(selectionKey.selector(), SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        keyToWriteByteArrayQueue.put(selectionKey1, new LinkedBlockingQueue<byte[]>());
    }

    private void handleWrite(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        byte[] writeByteArray = keyToWriteByteArrayQueue.get(selectionKey).poll();
        if (writeByteArray == null) {
            return;
        }
        //取太早缓存区会疏漏回收
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

    private void handleRead(SelectionKey selectionKey) throws IOException {
        StickyBuffer stickyBuffer = keyToReadBuffer.get(selectionKey);
        if (stickyBuffer == null)
            stickyBuffer = new StickyBuffer();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        int contentLength = stickyBuffer.getContentLength();
        ByteBuffer readBuffer = stickyBuffer.getByteBuffer();
        if (contentLength == -1) {
            socketChannel.read(headBuffer);
            if (!headBuffer.hasRemaining()) {
                headBuffer.flip();
                contentLength = headBuffer.getInt();
                stickyBuffer.setContentLength(contentLength);
                System.out.println(contentLength);
                stickyBuffer.setByteBuffer(ByteBuffer.allocate(contentLength));
                headBuffer.clear();
            }
        } else {
            socketChannel.read(readBuffer);
            if (!readBuffer.hasRemaining()) {
                Reply reply = eventHandler.onRead(readBuffer.array());
                if (reply.isWriteBack()) {
                    Queue<byte[]> queue = keyToWriteByteArrayQueue.get(selectionKey);
                    queue.offer(reply.getWriteBytes());
                }
                stickyBuffer.setByteBuffer(null);
                stickyBuffer.setContentLength(-1);
            }
        }
        keyToReadBuffer.put(selectionKey, stickyBuffer);
    }

    public void writeToAll(byte[] writeBytes) {
        for (Map.Entry<SelectionKey, Queue<byte[]>> entry : keyToWriteByteArrayQueue.entrySet()) {
            entry.getValue().offer(writeBytes);
        }
    }

}
