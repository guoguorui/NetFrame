package org.gary.netframe.buffer;

import java.util.LinkedList;
import java.util.Queue;

public class Pending {

    private Queue<byte[]> writeQueue=new LinkedList<>();

    private StickyBuffer stickyBuffer;

    public Queue<byte[]> getWriteQueue() {
        return writeQueue;
    }

    public void setWriteQueue(Queue<byte[]> writeQueue) {
        this.writeQueue = writeQueue;
    }

    public StickyBuffer getStickyBuffer() {
        return stickyBuffer;
    }

    public void setStickyBuffer(StickyBuffer stickyBuffer) {
        this.stickyBuffer = stickyBuffer;
    }
}
