package org.gary.netframe.eventloop;

import org.gary.netframe.eventhandler.EventHandler;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Set;

//一个EventLoopGroup负责多个EventLoop，每一个EventLoop使用一个线程
//EventLoopGroup使用阻塞队列通知EventLoop执行处理
public class EventLoopGroup{

    private static final int DEFAULT_LOOP_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors()*2);
    private ArrayList<EventLoop> list = new ArrayList<>(DEFAULT_LOOP_THREADS);

    public EventLoopGroup(EventHandler eventHandler) {
        for(int i = 0; i< DEFAULT_LOOP_THREADS; i++)
            list.add(new EventLoop(eventHandler));
    }

    public void dispatch(SelectionKey selectionKey){
        int index = selectionKey.hashCode() % DEFAULT_LOOP_THREADS;
        EventLoop eventLoop = list.get(index);
        try {
            eventLoop.queue.put(selectionKey);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendGroup(Set<SelectionKey> keyList, byte[] content){
        for(SelectionKey key : keyList){
            for(EventLoop eventLoop : list){
                if(eventLoop.map.get(key)!=null){
                    eventLoop.map.get(key).getWriteQueue().offer(content);
                    break;
                }
            }
        }
    }

}
