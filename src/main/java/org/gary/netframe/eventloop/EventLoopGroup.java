package org.gary.netframe.eventloop;

import org.gary.netframe.eventhandler.EventHandler;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;

//一个EventLoopGroup负责多个EventLoop，每一个EventLoop使用一个线程
//EventLoopGroup使用阻塞队列通知EventLoop执行处理
public class EventLoopGroup{

    private static final int LOOP_THREADS = Math.min(1, Runtime.getRuntime().availableProcessors()*2);

    private ArrayList<EventLoop> list = new ArrayList<>(LOOP_THREADS);

    public EventLoopGroup() {
        for(int i=0;i<LOOP_THREADS;i++)
            list.add(new EventLoop());
    }

    public void dispatch(SelectionKey selectionKey, EventHandler eventHandler){
        int index = selectionKey.hashCode() % LOOP_THREADS;
        EventLoop eventLoop = list.get(index);
        try {
            eventLoop.queue.put(new EventSource(selectionKey,eventHandler));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
