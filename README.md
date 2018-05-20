# NetFrame
服务端用法：
```
ServerEventHandler eventHandler=new MyServerEventHandler();
new NioServer(eventHandler).startup(8888);
```
其中的MyServerEventHandler继承ServerEventHandler，用户可以重写其中的onRead方法对接收到的数据进行处理
```
@Override
public Reply onRead(byte[] readBytes){
    String receive = new String(readBytes);
    System.out.println("server receive: " + receive);
    String[] ss = receive.split(" ");
    String id =ss[ss.length-1];
    return new Reply(true,("I am server, I got that "+id).getBytes());
}
```
其中的Reply代表了服务端对该客户端进行响应，如果不需要响应，则传入false和null参数<br><br>
服务端还可以对所有连接用户进行群发信息：
```
eventHandler.writeToAll("hello nico from server ".getBytes())
```
注意，用户的写操作都会返回一个boolean，如果该值是false，则代表连接已中断，发送无效<br><br>
客户端用法：
```
ClientEventHandler eventHandler=new MyClientEventHandler();
new NioClient(eventHandler).startup("127.0.0.1",8888);
eventHandler.writeToServer("hell nico from client")
```
