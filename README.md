# NetFrame
usage of server:
```
    org.gary.netframe.eventhandler.EventHandler eventHandler=new org.gary.netframe.test.server.ServerEventHandler();
    org.gary.netframe.nio.NioServer nioServer=new org.gary.netframe.nio.NioServer(eventHandler,10).startup(8080);
```
you should extends abstract class org.gary.netframe.eventhandler.EventHandler, and override method public org.gary.netframe.eventhandler.Reply onRead(byte[] readBytes){}, and if you want to write back to the remote peer after receiving data, you should return a org.gary.netframe.eventhandler.Reply object which the second parameter contains the data to send.
```
    @Override
    public org.gary.netframe.eventhandler.Reply onRead(byte[] readBytes) {
        System.out.println("client receive: "+new String(readBytes));
        return new org.gary.netframe.eventhandler.Reply(true,"hello nico from client".getBytes());
    }
```


if you want to write data after bootstrap and startup the server, it must be noted that this sentence would send the data to all the connected remote peer, if not remote peer exist, the data would be ignored. client can't do like this:
```
  nioServer.writeToAll("hello nico per second ".getBytes());
```


usage of client:
```
    org.gary.netframe.eventhandler.EventHandler eventHandler=new org.gary.netframe.test.client.ClientEventHandler();
    new org.gary.netframe.nio.NioClient(eventHandler).startup("127.0.0.1",8080);
```