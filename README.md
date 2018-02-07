# NetFrame
usage of server:
```
    EventHandler eventHandler=new ServerEventHandler();
    NioServer nioServer=new NioServer(eventHandler,10).startup(8080);
```
you should extends abstract class EventHandler, and override method public Reply onRead(byte[] readBytes){}, and if you want to write back to the remote peer after receiving data, you should return a Reply object which the second parameter contains the data to send.
```
    @Override
    public Reply onRead(byte[] readBytes) {
        System.out.println("client receive: "+new String(readBytes));
        return new Reply(true,"hello nico from client".getBytes());
    }
```


if you want to write data after bootstrap and startup the server, it must be noted that this sentence would send the data to all the connected remote peer, if not remote peer exist, the data would be ignored. client can't do like this:
```
  nioServer.writeToAll("hello nico per second ".getBytes());
```


usage of client:
```
    EventHandler eventHandler=new ClientEventHandler();
    new NioClient(eventHandler).startup("127.0.0.1",8080);
```