public class Server {

    public static void main(String[] args)
    {
        new BootStrapServer(new DefaultEventHandler()).startup(8080);
    }

}
