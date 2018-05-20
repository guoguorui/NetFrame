package org.gary.netframe.nio;

import org.gary.netframe.eventhandler.ServerEventHandler;
import org.gary.netframe.eventloop.EventLoopGroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

//考虑eventloop自动扩容
//考虑将ByteBuffer暴露给用户

public class NioServer {

    private EventLoopGroup eventLoopGroup;
    private Selector selector;
    private ServerEventHandler eventHandler;
    private volatile int connectAvailable;


    public NioServer(ServerEventHandler eventHandler) {
        this.eventHandler = eventHandler;
        eventLoopGroup = new EventLoopGroup(eventHandler);
    }

    public void startup(final int port) {
        new Thread(() -> {
            ServerSocketChannel serverSocketChannel = null;
            try {
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(port));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                connectAvailable = 1;
                while (!Thread.interrupted()) {
                    selector.select();
                    Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
                    while (selectionKeyIterator.hasNext()) {
                        SelectionKey selectionKey = selectionKeyIterator.next();
                        if(!selectionKey.isValid())
                            continue;
                        if(selectionKey.isAcceptable()){
                            handleAccept(selectionKey);
                        }else if(selectionKey.isReadable() || selectionKey.isWritable()){
                            eventLoopGroup.dispatch(selectionKey);
                        }
                        selectionKeyIterator.remove();
                    }
                }
            } catch (IOException e) {
                eventHandler.onException(e);
                //e.printStackTrace();
            } finally {
                connectAvailable = -1;
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
        eventHandler.onActive(this);
    }

    private void handleAccept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selectionKey.selector(), SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        Socket socket=socketChannel.socket();
        System.out.println("与客户端建立连接"+socket.getInetAddress()+":"+socket.getPort());
    }

    public void writeToAll(byte[] content){
        eventLoopGroup.sendGroup(selector.keys(),content);
    }

    public void connectAvailable() throws Exception {
        while (connectAvailable < 1) {
            if (connectAvailable == -1)
                throw new Exception("fail to connect");
        }
    }

}
