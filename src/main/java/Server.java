public class Server {

    public static void main(String[] args)
    {
        EventHandler eventHandler=new ServerEventHandler();
        new BootstrapServer(eventHandler,10).startup(8080);
        eventHandler.writeToAll("hello nico after bootstrap".getBytes());
    }

}
