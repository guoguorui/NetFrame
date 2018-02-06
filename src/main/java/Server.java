public class Server {

    public static void main(String[] args)
    {
        EventHandler eventHandler=new ServerEventHandler();
        NioServer nioServer=new NioServer(eventHandler,10).startup(8080);
        for(int i=0;i<100;i++){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nioServer.writeToAll(new String("hello nico per second "+i).getBytes());
            System.out.println("server queue: "+i);
        }
    }

}
