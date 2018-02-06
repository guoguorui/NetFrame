public class Client {

    public static void main(String[] args){
        EventHandler eventHandler=new ClientEventHandler();
        new NioClient(eventHandler).startup("127.0.0.1",8080);
    }
}
