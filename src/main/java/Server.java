public class Server {

    public static void main(String[] args)
    {
        new BootStrapServer(new MyEventHandler()).startup(8080);
    }

}
