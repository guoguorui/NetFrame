public class Server {

    public static void main(String[] args)
    {
        EventHandler eventHandler=new MyEventHandler();
        eventHandler.writeToAll("hello nico before bootstrap\n".getBytes());
        new BootStrapServer(eventHandler,10).startup(8080);
        eventHandler.writeToAll("hello nico after bootstrap".getBytes());
    }

}
