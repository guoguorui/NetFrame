public class Server {

    public static void main(String[] args)
    {
        EventHandler eventHandler=new MyEventHandler();
        eventHandler.write("hello nico before bootstrap\n".getBytes());
        new BootStrapServer(eventHandler).startup(8080);
        eventHandler.write("hello nico after bootstrap".getBytes());
    }

}
