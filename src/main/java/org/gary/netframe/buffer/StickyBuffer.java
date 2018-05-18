package org.gary.netframe.buffer;

import java.nio.ByteBuffer;

public class StickyBuffer {

    private ByteBuffer byteBuffer;
    private int contentLength=-1;

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
}
