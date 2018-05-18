# NetFrame
usage of server:
```
EventHandler eventHandler=new ServerEventHandler();
NioServer nioServer=new NioServer(eventHandler,10).startup(8888);
```
you should extends abstract class org.gary.netframe.eventhandler.EventHandler, and override method public org.gary.netframe.eventhandler.Reply onRead(byte[] readBytes){}, and if you want to write back to the remote peer after receiving data, you should return a org.gary.netframe.eventhandler.Reply object which the second parameter contains the data to send.
```
@Override
public Reply onRead(byte[] readBytes){
    System.out.println("server receive: "+new String(readBytes));
    return new Reply(true,"I am server, I got that".getBytes());
}
```


if you want to write data after bootstrap and startup the server, it must be noted that this sentence would send the data to all the connected remote peer, if not remote peer exist, the data would be ignored. client can't do like this:
```
nioServer.writeToAll("hello nico per second ".getBytes());
```


usage of client:
```
EventHandler eventHandler=new ClientEventHandler();
NioClient nioClient=new NioClient(eventHandler).startup("127.0.0.1",8888);
nioClient.writeToServer("hell nico from client".getBytes());
```