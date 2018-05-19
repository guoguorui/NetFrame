package org.gary.netframe.eventloop;

import org.gary.netframe.eventhandler.EventHandler;

import java.nio.channels.SelectionKey;

class EventSource {

    private SelectionKey selectionKey;

    private EventHandler eventHandler;

    public EventSource(SelectionKey selectionKey, EventHandler eventHandler) {
        this.selectionKey = selectionKey;
        this.eventHandler = eventHandler;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }
}
